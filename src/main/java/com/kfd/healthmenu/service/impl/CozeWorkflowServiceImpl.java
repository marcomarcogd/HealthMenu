package com.kfd.healthmenu.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kfd.healthmenu.dto.CozeWorkflowRequest;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import com.kfd.healthmenu.service.CozeWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CozeWorkflowServiceImpl implements CozeWorkflowService {

    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\((https?://[^)]+)\\)");

    @Value("${app.coze.enabled:false}")
    private boolean enabled;

    @Value("${app.coze.text-import-url:}")
    private String textImportUrl;

    @Value("${app.coze.text-import-token:}")
    private String textImportToken;

    @Value("${app.coze.image-url:}")
    private String imageUrl;

    @Value("${app.coze.image-token:}")
    private String imageToken;

    @Value("${app.coze.image-workflow:image}")
    private String imageWorkflowCode;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CozeWorkflowResponse execute(CozeWorkflowRequest request) {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        if (!enabled) {
            response.setRawResponse("Coze 未启用，请先配置工作流与鉴权信息");
            response.setErrorMessage("Coze 未启用，请先配置工作流与鉴权信息");
            response.setParsedText(request.getSourceText());
            return response;
        }

        String url = resolveUrl(request.getWorkflowCode());
        String token = resolveToken(request.getWorkflowCode());
        if (!StringUtils.hasText(url) || !StringUtils.hasText(token)) {
            response.setRawResponse("缺少 Coze URL 或 Token 配置");
            response.setErrorMessage("缺少 Coze URL 或 Token 配置");
            return response;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildPayload(request), headers);
            ResponseEntity<String> httpResponse = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String body = httpResponse.getBody();
            response.setRawResponse(body);
            parseResponse(body, response);
            response.setSuccess(resolveSuccessFlag(response, httpResponse.getStatusCode().is2xxSuccessful()));
            return response;
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            response.setRawResponse(body);
            parseResponse(body, response);
            response.setSuccess(false);
            if (!StringUtils.hasText(response.getErrorMessage())) {
                response.setErrorMessage("调用 Coze 工作流失败：" + ex.getStatusCode());
            }
            return response;
        } catch (Exception ex) {
            log.error("调用 Coze 工作流失败", ex);
            response.setRawResponse("调用 Coze 工作流失败：" + ex.getMessage());
            response.setErrorMessage("调用 Coze 工作流失败：" + ex.getMessage());
            return response;
        }
    }

    private String resolveUrl(String workflowCode) {
        if (isImageWorkflow(workflowCode)) {
            return imageUrl;
        }
        return textImportUrl;
    }

    private String resolveToken(String workflowCode) {
        if (isImageWorkflow(workflowCode)) {
            return imageToken;
        }
        return textImportToken;
    }

    private boolean isImageWorkflow(String workflowCode) {
        if (!StringUtils.hasText(workflowCode)) {
            return false;
        }
        if (StringUtils.hasText(imageWorkflowCode) && imageWorkflowCode.equalsIgnoreCase(workflowCode)) {
            return true;
        }
        return "image".equalsIgnoreCase(workflowCode);
    }

    private Map<String, Object> buildPayload(CozeWorkflowRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (isImageWorkflow(request.getWorkflowCode())) {
            payload.put("prompt", defaultObject(request.getPrompt()));
            payload.put("sceneType", defaultObject(request.getSceneType()));
            payload.put("styleHint", defaultObject(request.getStyleHint()));
            return payload;
        }
        payload.put("sourceText", StringUtils.hasText(request.getSourceText()) ? request.getSourceText() : "");
        payload.put("sourceImageUrl", defaultObject(request.getSourceImageUrl()));
        payload.put("templateHint", defaultObject(request.getTemplateHint()));
        payload.put("customerName", defaultObject(request.getCustomerName()));
        return payload;
    }

    private Object defaultObject(String value) {
        return StringUtils.hasText(value) ? value : new LinkedHashMap<>();
    }

    private void parseResponse(String body, CozeWorkflowResponse response) {
        if (!StringUtils.hasText(body)) {
            response.setParsedText(body);
            return;
        }

        String markdownImageUrl = extractImageUrl(body);
        if (!JSONUtil.isTypeJSON(body)) {
            response.setParsedText(body);
            if (StringUtils.hasText(markdownImageUrl)) {
                response.setImageUrl(markdownImageUrl);
            }
            return;
        }

        JSONObject jsonObject = JSONUtil.parseObj(body);
        response.setParsedText(extractMenuJson(jsonObject));
        response.setImageUrl(extractImageUrl(jsonObject));
        response.setTaskId(extractField(jsonObject, "taskId", "task_id", "run_id"));
        response.setPrompt(extractField(jsonObject, "prompt"));
        response.setErrorMessage(extractField(jsonObject, "msg", "message", "error", "errorMessage"));
        Boolean success = extractBoolean(jsonObject, "success", "ok");
        if (success != null) {
            response.setSuccess(success);
        }
        if (!StringUtils.hasText(response.getParsedText()) && !StringUtils.hasText(response.getImageUrl()) && !StringUtils.hasText(response.getErrorMessage())) {
            response.setParsedText(body);
        }
    }

    private String extractMenuJson(JSONObject jsonObject) {
        if (jsonObject.containsKey("menuJson")) {
            return normalizeJsonText(jsonObject.getStr("menuJson"));
        }
        if (jsonObject.containsKey("title") || jsonObject.containsKey("meals")) {
            return jsonObject.toString();
        }
        for (String key : new String[]{"data", "output", "result", "content"}) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject nested) {
                String nestedMenuJson = extractMenuJson(nested);
                if (StringUtils.hasText(nestedMenuJson)) {
                    return nestedMenuJson;
                }
            } else if (value instanceof String text) {
                if (JSONUtil.isTypeJSON(text)) {
                    JSONObject parsed = JSONUtil.parseObj(text);
                    String nestedMenuJson = extractMenuJson(parsed);
                    if (StringUtils.hasText(nestedMenuJson)) {
                        return nestedMenuJson;
                    }
                }
                String normalized = normalizeJsonText(text);
                if (StringUtils.hasText(normalized) && JSONUtil.isTypeJSON(normalized)) {
                    JSONObject parsed = JSONUtil.parseObj(normalized);
                    if (parsed.containsKey("title") || parsed.containsKey("meals")) {
                        return parsed.toString();
                    }
                }
            }
        }
        return null;
    }

    private String normalizeJsonText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\\\"", "\"");
        return normalized;
    }

    private String extractField(JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            String direct = jsonObject.getStr(key);
            if (StringUtils.hasText(direct)) {
                return direct;
            }
        }
        for (String key : new String[]{"data", "output", "result", "content"}) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject nested) {
                String nestedValue = extractField(nested, keys);
                if (StringUtils.hasText(nestedValue)) {
                    return nestedValue;
                }
            } else if (value instanceof JSONArray nestedArray) {
                for (Object item : nestedArray) {
                    if (item instanceof JSONObject nestedObject) {
                        String nestedValue = extractField(nestedObject, keys);
                        if (StringUtils.hasText(nestedValue)) {
                            return nestedValue;
                        }
                    } else if (item instanceof String text && StringUtils.hasText(text)) {
                        return text;
                    }
                }
            }
        }
        return null;
    }

    private Boolean extractBoolean(JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            Object value = jsonObject.get(key);
            if (value instanceof Boolean boolValue) {
                return boolValue;
            }
            if (value instanceof Number numberValue) {
                return numberValue.intValue() != 0;
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                if ("true".equalsIgnoreCase(text) || "success".equalsIgnoreCase(text) || "ok".equalsIgnoreCase(text)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(text) || "fail".equalsIgnoreCase(text) || "error".equalsIgnoreCase(text)) {
                    return false;
                }
            }
        }
        return null;
    }

    private boolean resolveSuccessFlag(CozeWorkflowResponse response, boolean httpSuccess) {
        if (Boolean.TRUE.equals(response.getSuccess())) {
            return true;
        }
        if (Boolean.FALSE.equals(response.getSuccess())) {
            return false;
        }
        if (!httpSuccess || StringUtils.hasText(response.getErrorMessage())) {
            return false;
        }
        return StringUtils.hasText(response.getParsedText()) || StringUtils.hasText(response.getImageUrl());
    }

    private String extractImageUrl(Object value) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : new String[]{"imageUrl", "image_url", "url", "image", "image_link"}) {
                String direct = jsonObject.getStr(key);
                if (looksLikeHttpUrl(direct)) {
                    return direct;
                }
            }
            for (String key : new String[]{"images", "imageList", "image_list", "urls"}) {
                Object nested = jsonObject.get(key);
                String nestedImageUrl = extractImageUrl(nested);
                if (StringUtils.hasText(nestedImageUrl)) {
                    return nestedImageUrl;
                }
            }
            for (String key : new String[]{"data", "output", "result", "content"}) {
                String nestedImageUrl = extractImageUrl(jsonObject.get(key));
                if (StringUtils.hasText(nestedImageUrl)) {
                    return nestedImageUrl;
                }
            }
            return null;
        }
        if (value instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                String nestedImageUrl = extractImageUrl(item);
                if (StringUtils.hasText(nestedImageUrl)) {
                    return nestedImageUrl;
                }
            }
            return null;
        }
        if (value instanceof JSON jsonValue) {
            return extractImageUrl(JSONUtil.parse(jsonValue));
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (looksLikeHttpUrl(trimmed)) {
                return trimmed;
            }
            Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(trimmed);
            if (markdownMatcher.find()) {
                return markdownMatcher.group(1);
            }
            if (JSONUtil.isTypeJSON(trimmed)) {
                return extractImageUrl(JSONUtil.parse(trimmed));
            }
        }
        return null;
    }

    private boolean looksLikeHttpUrl(String value) {
        return StringUtils.hasText(value)
                && (value.startsWith("http://") || value.startsWith("https://"));
    }
}
