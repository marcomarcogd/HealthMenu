package com.kfd.healthmenu.service.impl;

import cn.hutool.json.JSONUtil;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CozeWorkflowRequest;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import com.kfd.healthmenu.dto.CustomerMenuMealForm;
import com.kfd.healthmenu.dto.CustomerMenuMealItemForm;
import com.kfd.healthmenu.service.AiImportService;
import com.kfd.healthmenu.service.CozeWorkflowService;
import com.kfd.healthmenu.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiImportServiceImpl implements AiImportService {

    private static final Pattern WEEK_INDEX_PATTERN = Pattern.compile("第\\s*([一二三四五六七八九十百零两\\d]+)\\s*周");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([\\p{IsAlphabetic}\\p{IsIdeographic}A-Za-z]+)\\s*[：:|-]\\s*(.+)$");

    private final CozeWorkflowService cozeWorkflowService;
    private final FileStorageService fileStorageService;

    @Value("${app.coze.text-import-workflow:text}")
    private String textImportWorkflow;

    @Value("${app.coze.image-workflow:image}")
    private String imageWorkflow;

    @Value("${app.coze.image-style-hint:营养师海报风，干净、柔和、真实食材质感}")
    private String imageStyleHint;

    @Override
    public AiImportResultDto parseMenuText(String sourceText) {
        CozeWorkflowRequest request = new CozeWorkflowRequest();
        request.setWorkflowCode(textImportWorkflow);
        request.setSourceText(sourceText);

        CozeWorkflowResponse response = cozeWorkflowService.execute(request);
        AiImportResultDto parsed = parseCozeResponse(response, sourceText);
        if (parsed != null) {
            return parsed;
        }

        return heuristicByText(sourceText, resolveTextFallbackMessage(response));
    }

    @Override
    public String generateImage(String prompt) {
        CozeWorkflowRequest request = new CozeWorkflowRequest();
        request.setWorkflowCode(imageWorkflow);
        request.setPrompt(prompt);
        request.setSceneType("meal_item");
        request.setStyleHint(imageStyleHint);

        CozeWorkflowResponse response = cozeWorkflowService.execute(request);
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new BizException("AI_IMAGE_FAILED", resolveImageFailureMessage(response));
        }
        if (!StringUtils.hasText(response.getImageUrl())) {
            throw new BizException("AI_IMAGE_FAILED", "Coze 生图成功但未返回可用图片地址，请检查工作流输出字段");
        }
        try {
            return fileStorageService.downloadToLocal(response.getImageUrl(), null);
        } catch (Exception ex) {
            throw new BizException("AI_IMAGE_FAILED", "Coze 图片已生成，但保存到本地失败：" + ex.getMessage());
        }
    }

    private AiImportResultDto parseCozeResponse(CozeWorkflowResponse response, String sourceText) {
        if (response == null) {
            return null;
        }

        if (StringUtils.hasText(response.getParsedText())) {
            AiImportResultDto dto = parseStructuredPayload(response.getParsedText(), sourceText);
            if (dto != null) {
                dto.setParseMode("AI");
                dto.setParseMessage("已使用 AI 结构化识别结果");
                return dto;
            }
        }

        if (StringUtils.hasText(response.getRawResponse())) {
            AiImportResultDto dto = parseStructuredPayload(response.getRawResponse(), sourceText);
            if (dto != null) {
                dto.setParseMode("AI");
                dto.setParseMessage("已从 AI 原始返回中提取结构化结果");
                return dto;
            }
        }

        return null;
    }

    private String resolveTextFallbackMessage(CozeWorkflowResponse response) {
        String defaultMessage = "AI 未返回可用的结构化结果，已按文本规则提取";
        if (response == null) {
            return defaultMessage;
        }
        String detail = firstNonBlank(response.getErrorMessage(), response.getRawResponse());
        if (!StringUtils.hasText(detail)) {
            return defaultMessage;
        }
        return defaultMessage + "。原因：" + truncate(detail.replaceAll("\\s+", " ").trim(), 80);
    }

    private String resolveImageFailureMessage(CozeWorkflowResponse response) {
        String defaultMessage = "AI 生图失败，请稍后重试或改用手动上传";
        if (response == null) {
            return defaultMessage;
        }
        String detail = firstNonBlank(response.getErrorMessage(), response.getRawResponse());
        if (!StringUtils.hasText(detail)) {
            return defaultMessage;
        }
        return "AI 生图失败：" + truncate(detail.replaceAll("\\s+", " ").trim(), 80);
    }

    private AiImportResultDto parseStructuredPayload(String payload, String fallbackText) {
        String normalized = unwrapStructuredText(payload);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        try {
            if (JSONUtil.isTypeJSON(normalized)) {
                AiImportResultDto dto = JSONUtil.toBean(normalized, AiImportResultDto.class);
                normalizeResult(dto, fallbackText);
                if (hasMeaningfulResult(dto)) {
                    return dto;
                }
            }
        } catch (Exception ignored) {
        }

        if (looksLikePlainMenuText(normalized)) {
            return heuristicByText(normalized, "AI 返回了非标准 JSON，已按文本规则提取");
        }

        return null;
    }

    private void normalizeResult(AiImportResultDto dto, String fallbackText) {
        if (dto == null) {
            return;
        }

        if (!StringUtils.hasText(dto.getTitle())) {
            dto.setTitle(extractTitle(fallbackText));
        }
        if (dto.getWeekIndex() == null) {
            dto.setWeekIndex(extractWeekIndex(fallbackText));
        }
        if (!StringUtils.hasText(dto.getWeeklyTip())) {
            dto.setWeeklyTip(extractSectionText(fallbackText, "每周提示", "建议", "提醒"));
        }
        if (!StringUtils.hasText(dto.getSwapGuide())) {
            dto.setSwapGuide(extractSectionText(fallbackText, "互换", "替换", "等量"));
        }
        normalizeMealStyles(dto);
    }

    private boolean hasMeaningfulResult(AiImportResultDto dto) {
        if (dto == null) {
            return false;
        }

        return StringUtils.hasText(dto.getTitle())
                || StringUtils.hasText(dto.getWeeklyTip())
                || StringUtils.hasText(dto.getSwapGuide())
                || (dto.getMeals() != null && !dto.getMeals().isEmpty());
    }

    private String unwrapStructuredText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized
                    .replaceFirst("^```(?:json|JSON|text)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace("\\\"", "\"");
    }

    private boolean looksLikePlainMenuText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("早餐")
                || normalized.contains("午餐")
                || normalized.contains("晚餐")
                || normalized.contains("加餐")
                || normalized.contains("每周提示")
                || normalized.contains("互换");
    }

    private AiImportResultDto heuristicByText(String sourceText, String parseMessage) {
        AiImportResultDto dto = new AiImportResultDto();
        dto.setParseMode("HEURISTIC");
        dto.setParseMessage(parseMessage);
        dto.setTitle(extractTitle(sourceText));
        dto.setWeekIndex(extractWeekIndex(sourceText));
        dto.setWeeklyTip(extractSectionText(sourceText, "每周提示", "建议", "提醒"));
        dto.setSwapGuide(extractSectionText(sourceText, "互换", "替换", "等量"));
        dto.setMeals(extractMeals(sourceText));
        normalizeMealStyles(dto);

        if (!hasMeaningfulResult(dto)) {
            return buildLooseFallback(sourceText);
        }
        return dto;
    }

    private AiImportResultDto buildLooseFallback(String sourceText) {
        AiImportResultDto dto = new AiImportResultDto();
        dto.setParseMode("FALLBACK");
        dto.setParseMessage("未识别出明确餐次，未再填充示例餐单，请手动补充或调整原始文本后重试");
        dto.setTitle(extractTitle(sourceText));
        dto.setWeekIndex(extractWeekIndex(sourceText));
        dto.setWeeklyTip(extractSectionText(sourceText, "每周提示", "建议", "提醒"));
        dto.setSwapGuide(extractSectionText(sourceText, "互换", "替换", "等量"));
        dto.setMeals(new ArrayList<>());
        normalizeMealStyles(dto);
        return dto;
    }

    private String extractTitle(String sourceText) {
        if (!StringUtils.hasText(sourceText)) {
            return "AI 识别餐单";
        }

        for (String line : splitLines(sourceText)) {
            if (line.contains("标题")) {
                String value = extractValue(line);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }

        String firstLine = splitLines(sourceText).stream()
                .filter(StringUtils::hasText)
                .filter(line -> !isMealHeading(line))
                .findFirst()
                .orElse("");
        return StringUtils.hasText(firstLine) ? truncate(firstLine, 24) : "AI 识别餐单";
    }

    private Integer extractWeekIndex(String sourceText) {
        if (!StringUtils.hasText(sourceText)) {
            return 1;
        }

        Matcher matcher = WEEK_INDEX_PATTERN.matcher(sourceText);
        if (matcher.find()) {
            return Math.max(1, parseChineseNumber(matcher.group(1)));
        }
        return 1;
    }

    private String extractSectionText(String sourceText, String... keywords) {
        if (!StringUtils.hasText(sourceText)) {
            return "";
        }

        for (String line : splitLines(sourceText)) {
            for (String keyword : keywords) {
                if (line.contains(keyword)) {
                    String value = extractValue(line);
                    return StringUtils.hasText(value) ? value : line;
                }
            }
        }
        return "";
    }

    private List<CustomerMenuMealForm> extractMeals(String sourceText) {
        List<CustomerMenuMealForm> meals = new ArrayList<>();
        if (!StringUtils.hasText(sourceText)) {
            return meals;
        }

        Map<String, CustomerMenuMealForm> mealMap = new LinkedHashMap<>();
        CustomerMenuMealForm currentMeal = null;

        for (String rawLine : splitLines(sourceText)) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }

            MealDescriptor descriptor = matchMeal(line);
            if (descriptor != null) {
                currentMeal = mealMap.computeIfAbsent(descriptor.code(), key -> createMeal(descriptor, mealMap.size() + 1));
                String remainder = stripMealHeading(line, descriptor.displayName());
                if (StringUtils.hasText(remainder)) {
                    addMealContent(currentMeal, remainder);
                }
                continue;
            }

            if (currentMeal != null) {
                addMealContent(currentMeal, line);
            }
        }

        meals.addAll(mealMap.values());
        return meals;
    }

    private CustomerMenuMealForm createMeal(MealDescriptor descriptor, int sortOrder) {
        CustomerMenuMealForm meal = new CustomerMenuMealForm();
        meal.setMealCode(descriptor.code());
        meal.setMealName(descriptor.displayName());
        meal.setTimeLabel(descriptor.displayName() + "时间");
        meal.setSortOrder(sortOrder);
        return meal;
    }

    private void addMealContent(CustomerMenuMealForm meal, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }

        String cleaned = text.trim()
                .replace("；", ";")
                .replace("，", ",");

        Matcher matcher = KEY_VALUE_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            addMealItem(meal, matcher.group(1), matcher.group(2));
            return;
        }

        String[] chunks = cleaned.split("[;；]");
        if (chunks.length > 1) {
            for (String chunk : chunks) {
                if (StringUtils.hasText(chunk)) {
                    addMealContent(meal, chunk);
                }
            }
            return;
        }

        if (cleaned.matches("^(\\d{1,2}:\\d{2}).*")) {
            if (!StringUtils.hasText(meal.getMealTime())) {
                meal.setMealTime(cleaned.substring(0, 5));
            }
            String remainder = cleaned.substring(Math.min(cleaned.length(), 5)).replaceFirst("^[：:，,\\s]+", "").trim();
            if (StringUtils.hasText(remainder)) {
                addMealItem(meal, "内容", remainder);
            }
            return;
        }

        addMealItem(meal, "内容", cleaned);
    }

    private void addMealItem(CustomerMenuMealForm meal, String itemName, String itemValue) {
        if (!StringUtils.hasText(itemValue)) {
            return;
        }

        CustomerMenuMealItemForm item = new CustomerMenuMealItemForm();
        item.setItemCode(normalizeCode(itemName));
        item.setItemName(normalizeItemName(itemName));
        item.setItemValue(itemValue.trim());
        item.setSortOrder(meal.getItems().size() + 1);
        item.setBold(false);
        item.setColor("#2d2d2d");
        meal.getItems().add(item);
    }

    private String normalizeItemName(String rawName) {
        if (!StringUtils.hasText(rawName) || "内容".equals(rawName)) {
            return "内容";
        }
        return rawName.trim();
    }

    private String normalizeCode(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return "content";
        }

        String normalized = rawName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("主食")) {
            return "staple";
        }
        if (normalized.contains("蛋白") || normalized.contains("肉") || normalized.contains("鱼") || normalized.contains("奶")) {
            return "protein";
        }
        if (normalized.contains("蔬") || normalized.contains("菜")) {
            return "vegetable";
        }
        if (normalized.contains("水果") || normalized.contains("果")) {
            return "fruit";
        }
        return normalized.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "_");
    }

    private String stripMealHeading(String line, String displayName) {
        String cleaned = line.replaceFirst("^\\s*" + Pattern.quote(displayName) + "\\s*", "");
        return cleaned.replaceFirst("^[：:|-]\\s*", "").trim();
    }

    private boolean isMealHeading(String line) {
        return matchMeal(line) != null;
    }

    private MealDescriptor matchMeal(String line) {
        if (!StringUtils.hasText(line)) {
            return null;
        }

        String normalized = line.trim();
        if (normalized.startsWith("早餐")) {
            return new MealDescriptor("breakfast", "早餐");
        }
        if (normalized.startsWith("上午加餐")) {
            return new MealDescriptor("morning_snack", "上午加餐");
        }
        if (normalized.startsWith("午餐")) {
            return new MealDescriptor("lunch", "午餐");
        }
        if (normalized.startsWith("下午加餐")) {
            return new MealDescriptor("afternoon_snack", "下午加餐");
        }
        if (normalized.startsWith("晚餐")) {
            return new MealDescriptor("dinner", "晚餐");
        }
        if (normalized.startsWith("晚间加餐") || normalized.startsWith("夜宵") || normalized.startsWith("宵夜")) {
            return new MealDescriptor("late_snack", "晚间加餐");
        }
        return null;
    }

    private List<String> splitLines(String sourceText) {
        List<String> lines = new ArrayList<>();
        if (!StringUtils.hasText(sourceText)) {
            return lines;
        }

        for (String line : sourceText.replace("\\n", "\n").split("\\r?\\n")) {
            String trimmed = line.trim();
            if (StringUtils.hasText(trimmed)) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private String extractValue(String line) {
        Matcher matcher = KEY_VALUE_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(2).trim();
        }
        int separatorIndex = Math.max(line.indexOf('：'), line.indexOf(':'));
        if (separatorIndex >= 0 && separatorIndex + 1 < line.length()) {
            return line.substring(separatorIndex + 1).trim();
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private int parseChineseNumber(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return 1;
        }

        if (rawValue.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(rawValue);
        }

        Map<Character, Integer> numberMap = Map.ofEntries(
                Map.entry('零', 0),
                Map.entry('一', 1),
                Map.entry('二', 2),
                Map.entry('两', 2),
                Map.entry('三', 3),
                Map.entry('四', 4),
                Map.entry('五', 5),
                Map.entry('六', 6),
                Map.entry('七', 7),
                Map.entry('八', 8),
                Map.entry('九', 9)
        );

        int result = 0;
        int section = 0;
        int number = 0;
        for (char ch : rawValue.toCharArray()) {
            if (numberMap.containsKey(ch)) {
                number = numberMap.get(ch);
            } else if (ch == '十') {
                section += (number == 0 ? 1 : number) * 10;
                number = 0;
            } else if (ch == '百') {
                section += (number == 0 ? 1 : number) * 100;
                number = 0;
            }
        }
        result += section + number;
        return result == 0 ? 1 : result;
    }

    private void normalizeMealStyles(AiImportResultDto dto) {
        if (dto == null || dto.getMeals() == null) {
            return;
        }

        for (CustomerMenuMealForm meal : dto.getMeals()) {
            if (meal.getItems() == null) {
                meal.setItems(new ArrayList<>());
                continue;
            }
            for (CustomerMenuMealItemForm item : meal.getItems()) {
                if (!StringUtils.hasText(item.getColor())) {
                    item.setColor("#2d2d2d");
                }
                if (item.getBold() == null) {
                    item.setBold(false);
                }
            }
        }
    }

    private record MealDescriptor(String code, String displayName) {
    }
}
