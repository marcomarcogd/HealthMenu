package com.kfd.healthmenu.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.common.ExportType;
import com.kfd.healthmenu.common.RecordStatus;
import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.CustomerMenuMealForm;
import com.kfd.healthmenu.dto.CustomerMenuMealItemForm;
import com.kfd.healthmenu.dto.CustomerMenuSectionForm;
import com.kfd.healthmenu.dto.TextStyleDto;
import com.kfd.healthmenu.dto.api.PageResult;
import com.kfd.healthmenu.dto.menu.CustomerMenuSummaryDto;
import com.kfd.healthmenu.dto.menu.MenuSummaryQuery;
import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.entity.CustomerMenuMeal;
import com.kfd.healthmenu.entity.CustomerMenuMealItem;
import com.kfd.healthmenu.entity.CustomerMenuSectionContent;
import com.kfd.healthmenu.entity.MenuPublishRecord;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.entity.MenuTemplateMeal;
import com.kfd.healthmenu.entity.MenuTemplateMealItem;
import com.kfd.healthmenu.entity.MenuTemplateSection;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMealItemMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMealMapper;
import com.kfd.healthmenu.mapper.CustomerMenuSectionContentMapper;
import com.kfd.healthmenu.mapper.MenuPublishRecordMapper;
import com.kfd.healthmenu.service.AiImportService;
import com.kfd.healthmenu.service.CustomerMenuService;
import com.kfd.healthmenu.service.ExportService;
import com.kfd.healthmenu.service.TemplateService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class CustomerMenuServiceImpl implements CustomerMenuService {

    private final CustomerMenuMapper customerMenuMapper;
    private final CustomerMenuSectionContentMapper sectionContentMapper;
    private final CustomerMenuMealMapper mealMapper;
    private final CustomerMenuMealItemMapper mealItemMapper;
    private final CustomerMapper customerMapper;
    private final MenuPublishRecordMapper menuPublishRecordMapper;
    private final TemplateService templateService;
    private final AiImportService aiImportService;
    private final ExportService exportService;

    private static final String DEFAULT_OPERATOR = "admin-ui";

    @Override
    public List<CustomerMenu> listAll() {
        return customerMenuMapper.selectList(new LambdaQueryWrapper<CustomerMenu>()
                .eq(CustomerMenu::getDeleted, 0)
                .orderByDesc(CustomerMenu::getMenuDate)
                .orderByDesc(CustomerMenu::getUpdateTime));
    }

    @Override
    public PageResult<CustomerMenuSummaryDto> listSummaries(MenuSummaryQuery query) {
        MenuSummaryQuery safeQuery = query == null ? new MenuSummaryQuery() : query;
        long pageNumber = normalizePageNumber(safeQuery.getPage());
        long pageSize = normalizePageSize(safeQuery.getPageSize());

        LambdaQueryWrapper<CustomerMenu> wrapper = new LambdaQueryWrapper<CustomerMenu>()
                .eq(CustomerMenu::getDeleted, 0);

        if (StringUtils.hasText(safeQuery.getStatus())) {
            wrapper.eq(CustomerMenu::getStatus, safeQuery.getStatus().trim());
        }

        if (safeQuery.getCustomerId() != null) {
            wrapper.eq(CustomerMenu::getCustomerId, safeQuery.getCustomerId());
        }

        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(q -> q.like(CustomerMenu::getTitle, keyword)
                    .or()
                    .like(CustomerMenu::getThemeCode, keyword)
                    .or()
                    .apply("CAST(menu_date AS CHAR) LIKE {0}", "%" + keyword + "%")
                    .or()
                    .apply("CAST(week_index AS CHAR) LIKE {0}", "%" + keyword + "%"));
        }

        applySummarySort(wrapper, safeQuery.getSort());

        long total = customerMenuMapper.selectCount(wrapper);
        if (total <= 0) {
            return new PageResult<>(Collections.emptyList(), 0L, pageNumber, pageSize);
        }

        long offset = (pageNumber - 1) * pageSize;
        wrapper.last("limit " + offset + ", " + pageSize);
        List<CustomerMenu> records = customerMenuMapper.selectList(wrapper);
        return new PageResult<>(
                records.stream().map(this::toSummaryDto).toList(),
                total,
                pageNumber,
                pageSize
        );
    }

    @Override
    public CustomerMenu getById(Long id) {
        return customerMenuMapper.selectById(id);
    }

    @Override
    public CustomerMenuForm buildCreateForm(Long customerId, Long templateId) {
        Customer customer = customerMapper.selectById(customerId);
        MenuTemplate template = templateService.getById(templateId);
        CustomerMenuForm form = baseCreateForm(customerId, templateId, customer, template);

        List<MenuTemplateSection> sections = templateService.listSections(templateId);
        for (MenuTemplateSection section : sections) {
            if (section.getEnabled() != null && section.getEnabled() == 0) {
                continue;
            }
            CustomerMenuSectionForm sectionForm = new CustomerMenuSectionForm();
            sectionForm.setSectionType(section.getSectionType());
            sectionForm.setTitle(section.getTitle());
            sectionForm.setSortOrder(section.getSortOrder());
            sectionForm.setBold(Boolean.FALSE);
            sectionForm.setColor("#2d2d2d");
            sectionForm.setAllowImage(isEnabled(section.getAllowImage()));
            if ("EXCLUSIVE_TITLE".equals(section.getSectionType()) && customer != null) {
                sectionForm.setContent(customer.getExclusiveTitle());
            }
            form.getSections().add(sectionForm);
        }

        fillMealsByTemplate(form, templateId, null);
        return normalizeForm(form, template);
    }

    @Override
    public CustomerMenuForm buildCreateFormFromAi(Long customerId, Long templateId, AiImportResultDto aiImportResultDto) {
        Customer customer = customerMapper.selectById(customerId);
        MenuTemplate template = templateService.getById(templateId);
        CustomerMenuForm form = baseCreateForm(customerId, templateId, customer, template);
        if (aiImportResultDto != null) {
            form.setTitle(StringUtils.hasText(aiImportResultDto.getTitle()) ? aiImportResultDto.getTitle() : form.getTitle());
            form.setWeekIndex(aiImportResultDto.getWeekIndex());
        }

        List<MenuTemplateSection> sections = templateService.listSections(templateId);
        for (MenuTemplateSection section : sections) {
            if (section.getEnabled() != null && section.getEnabled() == 0) {
                continue;
            }
            CustomerMenuSectionForm sectionForm = new CustomerMenuSectionForm();
            sectionForm.setSectionType(section.getSectionType());
            sectionForm.setTitle(section.getTitle());
            sectionForm.setSortOrder(section.getSortOrder());
            sectionForm.setBold(Boolean.FALSE);
            sectionForm.setColor("#2d2d2d");
            sectionForm.setAllowImage(isEnabled(section.getAllowImage()));
            if ("EXCLUSIVE_TITLE".equals(section.getSectionType())) {
                sectionForm.setContent(form.getTitle());
            } else if ("WEEKLY_TIP".equals(section.getSectionType()) && aiImportResultDto != null) {
                sectionForm.setContent(aiImportResultDto.getWeeklyTip());
            } else if ("SWAP_GUIDE".equals(section.getSectionType()) && aiImportResultDto != null) {
                sectionForm.setContent(aiImportResultDto.getSwapGuide());
            }
            form.getSections().add(sectionForm);
        }

        fillMealsByTemplate(form, templateId, aiImportResultDto);
        return normalizeForm(form, template);
    }

    @Override
    public AiImportResultDto parseAiMenuText(String sourceText) {
        return aiImportService.parseMenuText(sourceText);
    }

    @Override
    public String generateAiImage(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new BizException("AI_IMAGE_PROMPT_EMPTY", "请输入 AI 生图提示词");
        }
        String imagePath = aiImportService.generateImage(prompt.trim());
        if (!StringUtils.hasText(imagePath)) {
            throw new BizException("AI_IMAGE_FAILED", "AI 生图失败，请稍后重试或改用手动上传");
        }
        return imagePath;
    }

    @Override
    public CustomerMenuForm getFormById(Long id) {
        CustomerMenu menu = getById(id);
        if (menu == null) {
            return null;
        }
        MenuTemplate template = templateService.getById(menu.getTemplateId());
        CustomerMenuForm form = new CustomerMenuForm();
        form.setId(menu.getId());
        form.setCustomerId(menu.getCustomerId());
        form.setTemplateId(menu.getTemplateId());
        form.setMenuDate(menu.getMenuDate());
        form.setWeekIndex(menu.getWeekIndex());
        form.setTitle(menu.getTitle());
        form.setThemeCode(menu.getThemeCode());
        form.setShowWeeklyTip(menu.getShowWeeklyTip() != null && menu.getShowWeeklyTip() == 1);
        form.setShowSwapGuide(menu.getShowSwapGuide() != null && menu.getShowSwapGuide() == 1);
        form.setStatus(menu.getStatus());

        List<CustomerMenuSectionContent> sections = sectionContentMapper.selectList(new LambdaQueryWrapper<CustomerMenuSectionContent>()
                .eq(CustomerMenuSectionContent::getCustomerMenuId, id)
                .eq(CustomerMenuSectionContent::getDeleted, 0)
                .orderByAsc(CustomerMenuSectionContent::getSortOrder));
        for (CustomerMenuSectionContent section : sections) {
            CustomerMenuSectionForm sectionForm = new CustomerMenuSectionForm();
            sectionForm.setSectionType(section.getSectionType());
            sectionForm.setTitle(section.getTitle());
            sectionForm.setContent(section.getContent());
            sectionForm.setSortOrder(section.getSortOrder());
            sectionForm.setImagePath(section.getImagePath());
            sectionForm.setAiImagePrompt(section.getAiImagePrompt());
            fillStyle(section.getStyleJson(), sectionForm);
            form.getSections().add(sectionForm);
        }

        List<CustomerMenuMeal> meals = mealMapper.selectList(new LambdaQueryWrapper<CustomerMenuMeal>()
                .eq(CustomerMenuMeal::getCustomerMenuId, id)
                .eq(CustomerMenuMeal::getDeleted, 0)
                .orderByAsc(CustomerMenuMeal::getSortOrder));
        for (CustomerMenuMeal meal : meals) {
            CustomerMenuMealForm mealForm = new CustomerMenuMealForm();
            mealForm.setMealCode(meal.getMealCode());
            mealForm.setMealName(meal.getMealName());
            mealForm.setTimeLabel(meal.getTimeLabel());
            mealForm.setMealTime(meal.getMealTime());
            mealForm.setSortOrder(meal.getSortOrder());
            List<CustomerMenuMealItem> items = mealItemMapper.selectList(new LambdaQueryWrapper<CustomerMenuMealItem>()
                    .eq(CustomerMenuMealItem::getCustomerMenuMealId, meal.getId())
                    .eq(CustomerMenuMealItem::getDeleted, 0)
                    .orderByAsc(CustomerMenuMealItem::getSortOrder));
            for (CustomerMenuMealItem item : items) {
                CustomerMenuMealItemForm itemForm = new CustomerMenuMealItemForm();
                itemForm.setItemCode(item.getItemCode());
                itemForm.setItemName(item.getItemName());
                itemForm.setItemValue(item.getItemValue());
                itemForm.setSortOrder(item.getSortOrder());
                itemForm.setImagePath(item.getImagePath());
                itemForm.setAiImagePrompt(item.getAiImagePrompt());
                fillStyle(item.getStyleJson(), itemForm);
                mealForm.getItems().add(itemForm);
            }
            form.getMeals().add(mealForm);
        }
        applyTemplateImageFlags(form);
        return normalizeForm(form, template);
    }

    @Override
    @Transactional
    public Long saveMenu(CustomerMenuForm form) {
        CustomerMenu menu = form.getId() == null ? new CustomerMenu() : customerMenuMapper.selectById(form.getId());
        menu.setCustomerId(form.getCustomerId());
        menu.setTemplateId(form.getTemplateId());
        menu.setMenuDate(form.getMenuDate());
        menu.setWeekIndex(form.getWeekIndex());
        menu.setTitle(form.getTitle());
        menu.setShowWeeklyTip(Boolean.TRUE.equals(form.getShowWeeklyTip()) ? 1 : 0);
        menu.setShowSwapGuide(Boolean.TRUE.equals(form.getShowSwapGuide()) ? 1 : 0);
        menu.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : RecordStatus.DRAFT.name());
        if (!StringUtils.hasText(menu.getShareToken())) {
            menu.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        }
        MenuTemplate template = templateService.getById(form.getTemplateId());
        if (template != null) {
            menu.setThemeCode(template.getThemeCode());
        }

        if (menu.getId() == null) {
            customerMenuMapper.insert(menu);
        } else {
            customerMenuMapper.updateById(menu);
            clearSnapshot(menu.getId());
        }

        saveSections(menu.getId(), form.getSections());
        saveMeals(menu.getId(), form.getMeals());
        ensureSharePublishRecord(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void publishMenu(Long id) {
        CustomerMenu menu = requireMenu(id);
        publishMenuInternal(menu);
    }

    @Override
    @Transactional
    public void publishMenus(List<Long> ids) {
        for (CustomerMenu menu : requireMenus(ids)) {
            publishMenuInternal(menu);
        }
    }

    @Override
    @Transactional
    public void exportMenuExcel(Long id, HttpServletResponse response) {
        CustomerMenu menu = requireMenu(id);
        String fileName = exportService.exportMenuExcel(menu, response);
        recordPublish(menu.getId(), ExportType.EXCEL, null, fileName, DEFAULT_OPERATOR);
    }

    @Override
    @Transactional
    public void exportMenusExcel(List<Long> ids, HttpServletResponse response) {
        List<CustomerMenu> menus = requireMenus(ids);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''menu-batch-export.zip");

        Set<String> usedEntryNames = new HashSet<>();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            for (CustomerMenu menu : menus) {
                String entryName = resolveUniqueEntryName(usedEntryNames, exportService.buildMenuExcelFileName(menu));
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(exportService.buildMenuExcel(menu));
                zipOutputStream.closeEntry();
                recordPublish(menu.getId(), ExportType.EXCEL, null, entryName, DEFAULT_OPERATOR);
            }
            zipOutputStream.finish();
        } catch (IOException ex) {
            throw new IllegalStateException("批量导出 Excel 失败", ex);
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireMenu(id);
        clearSnapshot(id);
        List<MenuPublishRecord> publishRecords = menuPublishRecordMapper.selectList(new LambdaQueryWrapper<MenuPublishRecord>()
                .eq(MenuPublishRecord::getCustomerMenuId, id)
                .eq(MenuPublishRecord::getDeleted, 0));
        for (MenuPublishRecord record : publishRecords) {
            menuPublishRecordMapper.deleteById(record.getId());
        }
        customerMenuMapper.deleteById(id);
    }

    private CustomerMenuForm baseCreateForm(Long customerId, Long templateId, Customer customer, MenuTemplate template) {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(customerId);
        form.setTemplateId(templateId);
        form.setShowSwapGuide(true);
        form.setShowWeeklyTip(true);
        form.setStatus(RecordStatus.DRAFT.name());
        form.setTitle(buildDefaultTitle(customer, template));
        form.setThemeCode(template == null ? null : template.getThemeCode());
        return form;
    }

    private CustomerMenuForm normalizeForm(CustomerMenuForm form, MenuTemplate template) {
        if (form.getSections() == null) {
            form.setSections(new java.util.ArrayList<>());
        }
        if (form.getMeals() == null) {
            form.setMeals(new java.util.ArrayList<>());
        }
        if (form.getWeekIndex() == null) {
            form.setWeekIndex(1);
        }
        if (form.getShowWeeklyTip() == null) {
            form.setShowWeeklyTip(Boolean.TRUE);
        }
        if (form.getShowSwapGuide() == null) {
            form.setShowSwapGuide(Boolean.TRUE);
        }
        if (!StringUtils.hasText(form.getStatus())) {
            form.setStatus(RecordStatus.DRAFT.name());
        }
        if (!StringUtils.hasText(form.getThemeCode()) && template != null) {
            form.setThemeCode(template.getThemeCode());
        }
        applyTemplateImageFlags(form);
        form.setThemeName(resolveThemeName(form.getThemeCode(), template));
        form.setStatusLabel(resolveStatusLabel(form.getStatus()));
        if (form.getId() != null) {
            form.setViewUrl(buildViewUrl(form.getId()));
            CustomerMenu menu = getById(form.getId());
            if (menu != null && StringUtils.hasText(menu.getShareToken())) {
                form.setShareUrl(buildShareUrl(menu.getShareToken()));
            }
            form.setPublishCount((int) countPublishRecords(form.getId()));
            form.setLastPublishedAt(findLastPublishedAt(form.getId()));
        }
        return form;
    }

    private void fillMealsByTemplate(CustomerMenuForm form, Long templateId, AiImportResultDto aiImportResultDto) {
        List<MenuTemplateMeal> meals = templateService.listMeals(templateId);
        for (MenuTemplateMeal meal : meals) {
            if (meal.getEnabled() != null && meal.getEnabled() == 0) {
                continue;
            }
            CustomerMenuMealForm mealForm = new CustomerMenuMealForm();
            mealForm.setMealCode(meal.getMealCode());
            mealForm.setMealName(meal.getMealName());
            mealForm.setTimeLabel(meal.getTimeLabel());
            mealForm.setSortOrder(meal.getSortOrder());

            CustomerMenuMealForm aiMeal = findAiMeal(aiImportResultDto, meal.getMealCode(), meal.getMealName());
            if (aiMeal != null) {
                mealForm.setMealTime(aiMeal.getMealTime());
            }

            for (MenuTemplateMealItem item : templateService.listMealItems(meal.getId())) {
                if (item.getEnabled() != null && item.getEnabled() == 0) {
                    continue;
                }
                CustomerMenuMealItemForm itemForm = new CustomerMenuMealItemForm();
                itemForm.setItemCode(item.getItemCode());
                itemForm.setItemName(item.getItemName());
                itemForm.setSortOrder(item.getSortOrder());
                itemForm.setBold(Boolean.FALSE);
                itemForm.setColor("#2d2d2d");
                itemForm.setAllowImage(isEnabled(item.getAllowImage()));
                if (aiMeal != null) {
                    CustomerMenuMealItemForm aiItem = findAiItem(aiMeal, item.getItemCode(), item.getItemName());
                    if (aiItem != null) {
                        itemForm.setItemValue(aiItem.getItemValue());
                        itemForm.setItemName(StringUtils.hasText(aiItem.getItemName()) ? aiItem.getItemName() : itemForm.getItemName());
                    }
                }
                mealForm.getItems().add(itemForm);
            }
            form.getMeals().add(mealForm);
        }

        if (aiImportResultDto != null && aiImportResultDto.getMeals() != null) {
            for (CustomerMenuMealForm aiMeal : aiImportResultDto.getMeals()) {
                boolean exists = form.getMeals().stream().anyMatch(m -> sameText(m.getMealCode(), aiMeal.getMealCode()) || sameText(m.getMealName(), aiMeal.getMealName()));
                if (!exists) {
                    CustomerMenuMealForm extraMeal = new CustomerMenuMealForm();
                    extraMeal.setMealCode(aiMeal.getMealCode());
                    extraMeal.setMealName(aiMeal.getMealName());
                    extraMeal.setTimeLabel(aiMeal.getTimeLabel());
                    extraMeal.setMealTime(aiMeal.getMealTime());
                    extraMeal.setSortOrder(form.getMeals().size() + 1);
                    if (aiMeal.getItems() != null) {
                        for (CustomerMenuMealItemForm aiItem : aiMeal.getItems()) {
                            CustomerMenuMealItemForm itemForm = new CustomerMenuMealItemForm();
                            itemForm.setItemCode(aiItem.getItemCode());
                            itemForm.setItemName(aiItem.getItemName());
                            itemForm.setItemValue(aiItem.getItemValue());
                            itemForm.setBold(Boolean.FALSE);
                            itemForm.setColor("#2d2d2d");
                            itemForm.setAllowImage(Boolean.FALSE);
                            itemForm.setSortOrder(extraMeal.getItems().size() + 1);
                            extraMeal.getItems().add(itemForm);
                        }
                    }
                    form.getMeals().add(extraMeal);
                }
            }
        }
    }

    private CustomerMenuMealForm findAiMeal(AiImportResultDto result, String mealCode, String mealName) {
        if (result == null || result.getMeals() == null) {
            return null;
        }
        return result.getMeals().stream()
                .filter(item -> sameText(item.getMealCode(), mealCode) || sameText(item.getMealName(), mealName))
                .findFirst()
                .orElse(null);
    }

    private CustomerMenuMealItemForm findAiItem(CustomerMenuMealForm meal, String itemCode, String itemName) {
        if (meal.getItems() == null) {
            return null;
        }
        return meal.getItems().stream()
                .filter(item -> sameText(item.getItemCode(), itemCode) || sameText(item.getItemName(), itemName))
                .findFirst()
                .orElse(null);
    }

    private boolean sameText(String a, String b) {
        return StringUtils.hasText(a) && StringUtils.hasText(b) && a.equalsIgnoreCase(b);
    }

    private String buildDefaultTitle(Customer customer, MenuTemplate template) {
        if (customer == null) {
            return template == null ? "客户餐单" : template.getName();
        }
        if (StringUtils.hasText(customer.getExclusiveTitle())) {
            return customer.getExclusiveTitle();
        }
        return customer.getName() + "的专属餐单";
    }

    private void clearSnapshot(Long menuId) {
        List<CustomerMenuSectionContent> sections = sectionContentMapper.selectList(new LambdaQueryWrapper<CustomerMenuSectionContent>()
                .eq(CustomerMenuSectionContent::getCustomerMenuId, menuId)
                .eq(CustomerMenuSectionContent::getDeleted, 0));
        for (CustomerMenuSectionContent section : sections) {
            sectionContentMapper.deleteById(section.getId());
        }
        List<CustomerMenuMeal> meals = mealMapper.selectList(new LambdaQueryWrapper<CustomerMenuMeal>()
                .eq(CustomerMenuMeal::getCustomerMenuId, menuId)
                .eq(CustomerMenuMeal::getDeleted, 0));
        for (CustomerMenuMeal meal : meals) {
            List<CustomerMenuMealItem> items = mealItemMapper.selectList(new LambdaQueryWrapper<CustomerMenuMealItem>()
                    .eq(CustomerMenuMealItem::getCustomerMenuMealId, meal.getId())
                    .eq(CustomerMenuMealItem::getDeleted, 0));
            for (CustomerMenuMealItem item : items) {
                mealItemMapper.deleteById(item.getId());
            }
            mealMapper.deleteById(meal.getId());
        }
    }

    private void saveSections(Long menuId, List<CustomerMenuSectionForm> sections) {
        if (sections == null) {
            return;
        }
        for (CustomerMenuSectionForm form : sections) {
            CustomerMenuSectionContent content = new CustomerMenuSectionContent();
            content.setCustomerMenuId(menuId);
            content.setSectionType(form.getSectionType());
            content.setTitle(form.getTitle());
            content.setContent(form.getContent());
            content.setImagePath(form.getImagePath());
            content.setAiImagePrompt(form.getAiImagePrompt());
            content.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
            content.setStyleJson(toStyleJson(form.getBold(), form.getColor()));
            sectionContentMapper.insert(content);
        }
    }

    private void saveMeals(Long menuId, List<CustomerMenuMealForm> meals) {
        if (meals == null) {
            return;
        }
        for (CustomerMenuMealForm mealForm : meals) {
            CustomerMenuMeal meal = new CustomerMenuMeal();
            meal.setCustomerMenuId(menuId);
            meal.setMealCode(mealForm.getMealCode());
            meal.setMealName(mealForm.getMealName());
            meal.setTimeLabel(mealForm.getTimeLabel());
            meal.setMealTime(mealForm.getMealTime());
            meal.setSortOrder(mealForm.getSortOrder() == null ? 0 : mealForm.getSortOrder());
            mealMapper.insert(meal);

            for (CustomerMenuMealItemForm itemForm : mealForm.getItems() == null ? Collections.<CustomerMenuMealItemForm>emptyList() : mealForm.getItems()) {
                CustomerMenuMealItem item = new CustomerMenuMealItem();
                item.setCustomerMenuMealId(meal.getId());
                item.setItemCode(itemForm.getItemCode());
                item.setItemName(itemForm.getItemName());
                item.setItemValue(itemForm.getItemValue());
                item.setImagePath(itemForm.getImagePath());
                item.setAiImagePrompt(itemForm.getAiImagePrompt());
                item.setSortOrder(itemForm.getSortOrder() == null ? 0 : itemForm.getSortOrder());
                item.setStyleJson(toStyleJson(itemForm.getBold(), itemForm.getColor()));
                mealItemMapper.insert(item);
            }
        }
    }

    private String toStyleJson(Boolean bold, String color) {
        TextStyleDto style = new TextStyleDto();
        style.setBold(Boolean.TRUE.equals(bold));
        style.setColor(StringUtils.hasText(color) ? color : "#2d2d2d");
        return JSONUtil.toJsonStr(style);
    }

    private void fillStyle(String styleJson, CustomerMenuSectionForm form) {
        TextStyleDto style = parseStyle(styleJson);
        form.setBold(style.getBold());
        form.setColor(style.getColor());
    }

    private void fillStyle(String styleJson, CustomerMenuMealItemForm form) {
        TextStyleDto style = parseStyle(styleJson);
        form.setBold(style.getBold());
        form.setColor(style.getColor());
    }

    private void applyTemplateImageFlags(CustomerMenuForm form) {
        if (form.getSections() == null || form.getMeals() == null) {
            return;
        }
        if (form.getTemplateId() == null) {
            defaultImageFlags(form);
            return;
        }

        Map<String, Boolean> sectionFlags = new HashMap<>();
        Map<String, Boolean> sectionTypeFlags = new HashMap<>();
        for (MenuTemplateSection section : templateService.listSections(form.getTemplateId())) {
            Boolean allowImage = isEnabled(section.getAllowImage());
            sectionFlags.put(buildSectionKey(section.getSectionType(), section.getSortOrder()), allowImage);
            sectionTypeFlags.putIfAbsent(section.getSectionType(), allowImage);
        }
        for (CustomerMenuSectionForm section : form.getSections()) {
            if (section.getAllowImage() == null) {
                Boolean allowImage = sectionFlags.get(buildSectionKey(section.getSectionType(), section.getSortOrder()));
                if (allowImage == null) {
                    allowImage = sectionTypeFlags.get(section.getSectionType());
                }
                section.setAllowImage(Boolean.TRUE.equals(allowImage));
            }
        }

        Map<String, Boolean> itemFlags = new HashMap<>();
        Map<String, Boolean> itemNameFlags = new HashMap<>();
        for (MenuTemplateMeal meal : templateService.listMeals(form.getTemplateId())) {
            for (MenuTemplateMealItem item : templateService.listMealItems(meal.getId())) {
                Boolean allowImage = isEnabled(item.getAllowImage());
                itemFlags.put(buildMealItemKey(meal.getMealCode(), item.getItemCode()), allowImage);
                itemNameFlags.putIfAbsent(buildMealItemKey(meal.getMealCode(), item.getItemName()), allowImage);
            }
        }
        for (CustomerMenuMealForm meal : form.getMeals()) {
            if (meal.getItems() == null) {
                continue;
            }
            for (CustomerMenuMealItemForm item : meal.getItems()) {
                if (item.getAllowImage() == null) {
                    Boolean allowImage = itemFlags.get(buildMealItemKey(meal.getMealCode(), item.getItemCode()));
                    if (allowImage == null) {
                        allowImage = itemNameFlags.get(buildMealItemKey(meal.getMealCode(), item.getItemName()));
                    }
                    item.setAllowImage(Boolean.TRUE.equals(allowImage));
                }
            }
        }
    }

    private void defaultImageFlags(CustomerMenuForm form) {
        for (CustomerMenuSectionForm section : form.getSections()) {
            if (section.getAllowImage() == null) {
                section.setAllowImage(Boolean.FALSE);
            }
        }
        for (CustomerMenuMealForm meal : form.getMeals()) {
            if (meal.getItems() == null) {
                continue;
            }
            for (CustomerMenuMealItemForm item : meal.getItems()) {
                if (item.getAllowImage() == null) {
                    item.setAllowImage(Boolean.FALSE);
                }
            }
        }
    }

    private String buildSectionKey(String sectionType, Integer sortOrder) {
        return (sectionType == null ? "" : sectionType) + "#" + (sortOrder == null ? 0 : sortOrder);
    }

    private String buildMealItemKey(String mealCode, String itemCodeOrName) {
        return (mealCode == null ? "" : mealCode) + "#" + (itemCodeOrName == null ? "" : itemCodeOrName);
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private TextStyleDto parseStyle(String styleJson) {
        if (!StringUtils.hasText(styleJson)) {
            TextStyleDto style = new TextStyleDto();
            style.setBold(false);
            style.setColor("#2d2d2d");
            return style;
        }
        TextStyleDto style = JSONUtil.toBean(styleJson, TextStyleDto.class);
        if (style.getBold() == null) {
            style.setBold(false);
        }
        if (!StringUtils.hasText(style.getColor())) {
            style.setColor("#2d2d2d");
        }
        return style;
    }

    private CustomerMenuSummaryDto toSummaryDto(CustomerMenu menu) {
        CustomerMenuSummaryDto dto = new CustomerMenuSummaryDto();
        dto.setId(menu.getId());
        dto.setCustomerId(menu.getCustomerId());
        Customer customer = menu.getCustomerId() == null ? null : customerMapper.selectById(menu.getCustomerId());
        dto.setCustomerName(customer == null ? null : customer.getName());
        dto.setTemplateId(menu.getTemplateId());
        dto.setMenuDate(menu.getMenuDate());
        dto.setWeekIndex(menu.getWeekIndex());
        dto.setTitle(menu.getTitle());
        dto.setStatus(menu.getStatus());
        dto.setStatusLabel(resolveStatusLabel(menu.getStatus()));
        dto.setThemeCode(menu.getThemeCode());
        MenuTemplate template = templateService.getById(menu.getTemplateId());
        dto.setThemeName(resolveThemeName(menu.getThemeCode(), template));
        dto.setViewUrl(buildViewUrl(menu.getId()));
        dto.setShareUrl(buildShareUrl(menu.getShareToken()));
        dto.setPublishCount((int) countPublishRecords(menu.getId()));
        dto.setLastPublishedAt(findLastPublishedAt(menu.getId()));
        return dto;
    }

    private void applySummarySort(LambdaQueryWrapper<CustomerMenu> wrapper, String sort) {
        String sortValue = StringUtils.hasText(sort) ? sort.trim() : "menuDateDesc";
        switch (sortValue) {
            case "menuDateAsc" -> wrapper.orderByAsc(CustomerMenu::getMenuDate).orderByDesc(CustomerMenu::getUpdateTime);
            case "updatedDesc" -> wrapper.orderByDesc(CustomerMenu::getUpdateTime).orderByDesc(CustomerMenu::getMenuDate);
            case "titleAsc" -> wrapper.orderByAsc(CustomerMenu::getTitle).orderByDesc(CustomerMenu::getMenuDate);
            default -> wrapper.orderByDesc(CustomerMenu::getMenuDate).orderByDesc(CustomerMenu::getUpdateTime);
        }
    }

    private long normalizePageNumber(long pageNumber) {
        return Math.max(pageNumber, 1L);
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize <= 0) {
            return 10L;
        }
        return Math.min(pageSize, 50L);
    }

    private CustomerMenu requireMenu(Long id) {
        CustomerMenu menu = customerMenuMapper.selectById(id);
        if (menu == null || menu.getDeleted() != null && menu.getDeleted() == 1) {
            throw new BizException("MENU_NOT_FOUND", "未找到对应的餐单");
        }
        return menu;
    }

    private List<CustomerMenu> requireMenus(List<Long> ids) {
        List<Long> safeIds = ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (safeIds.isEmpty()) {
            throw new BizException("MENU_IDS_REQUIRED", "请至少选择一条餐单");
        }

        List<CustomerMenu> menus = customerMenuMapper.selectBatchIds(safeIds).stream()
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .toList();
        if (menus.size() != safeIds.size()) {
            throw new BizException("MENU_NOT_FOUND", "部分餐单不存在或已删除");
        }

        Map<Long, CustomerMenu> menuMap = new LinkedHashMap<>();
        for (CustomerMenu menu : menus) {
            menuMap.put(menu.getId(), menu);
        }
        return safeIds.stream().map(menuMap::get).toList();
    }

    private void publishMenuInternal(CustomerMenu menu) {
        if (!StringUtils.hasText(menu.getShareToken())) {
            menu.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        }
        menu.setStatus(RecordStatus.PUBLISHED.name());
        customerMenuMapper.updateById(menu);
        String recordName = StringUtils.hasText(menu.getTitle()) ? menu.getTitle() + "-share-link" : "menu-share-link";
        recordPublish(menu.getId(), ExportType.SHARE_LINK, buildShareUrl(menu.getShareToken()), recordName, DEFAULT_OPERATOR);
    }

    private String resolveUniqueEntryName(Set<String> usedEntryNames, String rawFileName) {
        String safeFileName = sanitizeFileName(rawFileName);
        if (usedEntryNames.add(safeFileName)) {
            return safeFileName;
        }

        int suffix = 2;
        int dotIndex = safeFileName.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? safeFileName.substring(0, dotIndex) : safeFileName;
        String extension = dotIndex >= 0 ? safeFileName.substring(dotIndex) : "";
        String candidate = safeFileName;
        while (!usedEntryNames.add(candidate)) {
            candidate = baseName + "-" + suffix++ + extension;
        }
        return candidate;
    }

    private String sanitizeFileName(String rawFileName) {
        String fileName = StringUtils.hasText(rawFileName) ? rawFileName.trim() : "餐单.xlsx";
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void recordPublish(Long menuId, ExportType exportType, String filePath, String fileName, String operatorName) {
        MenuPublishRecord record = new MenuPublishRecord();
        record.setCustomerMenuId(menuId);
        record.setExportType(exportType.name());
        record.setFilePath(filePath);
        record.setFileName(fileName);
        record.setOperatorName(operatorName);
        menuPublishRecordMapper.insert(record);
    }

    private void ensureSharePublishRecord(CustomerMenu menu) {
        if (menu == null || !RecordStatus.PUBLISHED.name().equals(menu.getStatus())) {
            return;
        }
        if (countPublishRecords(menu.getId()) > 0) {
            return;
        }
        String recordName = StringUtils.hasText(menu.getTitle()) ? menu.getTitle() + "-share-link" : "menu-share-link";
        recordPublish(menu.getId(), ExportType.SHARE_LINK, buildShareUrl(menu.getShareToken()), recordName, DEFAULT_OPERATOR);
    }

    private long countPublishRecords(Long menuId) {
        Long count = menuPublishRecordMapper.selectCount(new LambdaQueryWrapper<MenuPublishRecord>()
                .eq(MenuPublishRecord::getCustomerMenuId, menuId)
                .eq(MenuPublishRecord::getExportType, ExportType.SHARE_LINK.name())
                .eq(MenuPublishRecord::getDeleted, 0));
        return count == null ? 0L : count;
    }

    private LocalDateTime findLastPublishedAt(Long menuId) {
        MenuPublishRecord record = menuPublishRecordMapper.selectOne(new LambdaQueryWrapper<MenuPublishRecord>()
                .eq(MenuPublishRecord::getCustomerMenuId, menuId)
                .eq(MenuPublishRecord::getExportType, ExportType.SHARE_LINK.name())
                .eq(MenuPublishRecord::getDeleted, 0)
                .orderByDesc(MenuPublishRecord::getCreateTime)
                .last("limit 1"));
        return record == null ? null : record.getCreateTime();
    }

    private String resolveStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "草稿";
        }
        try {
            return switch (RecordStatus.valueOf(status)) {
                case DRAFT -> "草稿";
                case PUBLISHED -> "已发布";
            };
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    private String resolveThemeName(String themeCode, MenuTemplate template) {
        if (template != null && StringUtils.hasText(template.getName())) {
            return template.getName();
        }
        return themeCode;
    }

    private String buildViewUrl(Long id) {
        return id == null ? null : "/view/menu/" + id;
    }

    private String buildShareUrl(String shareToken) {
        return StringUtils.hasText(shareToken) ? "/share/menu/" + shareToken : null;
    }
}
