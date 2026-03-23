package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.MenuTemplateDesignForm;
import com.kfd.healthmenu.dto.MenuTemplateMealDesignForm;
import com.kfd.healthmenu.dto.MenuTemplateMealItemDesignForm;
import com.kfd.healthmenu.dto.MenuTemplateSectionDesignForm;
import com.kfd.healthmenu.dto.template.MenuTemplateDesignDto;
import com.kfd.healthmenu.dto.template.MenuTemplateMealDto;
import com.kfd.healthmenu.dto.template.MenuTemplateMealItemDto;
import com.kfd.healthmenu.dto.template.MenuTemplateSectionDto;
import com.kfd.healthmenu.dto.template.TemplateDesignSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateMealItemSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateMealSaveRequest;
import com.kfd.healthmenu.dto.template.TemplatePreviewDto;
import com.kfd.healthmenu.dto.template.TemplateSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateSectionSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateSummaryDto;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.entity.MenuTemplateMeal;
import com.kfd.healthmenu.entity.MenuTemplateMealItem;
import com.kfd.healthmenu.entity.MenuTemplateSection;
import com.kfd.healthmenu.mapper.MenuTemplateMapper;
import com.kfd.healthmenu.mapper.MenuTemplateMealItemMapper;
import com.kfd.healthmenu.mapper.MenuTemplateMealMapper;
import com.kfd.healthmenu.mapper.MenuTemplateSectionMapper;
import com.kfd.healthmenu.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final MenuTemplateMapper menuTemplateMapper;
    private final MenuTemplateSectionMapper sectionMapper;
    private final MenuTemplateMealMapper mealMapper;
    private final MenuTemplateMealItemMapper mealItemMapper;

    @Override
    public List<MenuTemplate> listAll() {
        return menuTemplateMapper.selectList(new LambdaQueryWrapper<MenuTemplate>()
                .eq(MenuTemplate::getDeleted, 0)
                .orderByDesc(MenuTemplate::getIsDefault)
                .orderByDesc(MenuTemplate::getUpdateTime));
    }

    @Override
    public List<TemplateSummaryDto> listSummaries() {
        return listAll().stream().map(this::toSummaryDto).toList();
    }

    @Override
    public MenuTemplate getById(Long id) {
        return menuTemplateMapper.selectById(id);
    }

    @Override
    public List<MenuTemplateSection> listSections(Long templateId) {
        return sectionMapper.selectList(new LambdaQueryWrapper<MenuTemplateSection>()
                .eq(MenuTemplateSection::getTemplateId, templateId)
                .eq(MenuTemplateSection::getDeleted, 0)
                .orderByAsc(MenuTemplateSection::getSortOrder));
    }

    @Override
    public List<MenuTemplateMeal> listMeals(Long templateId) {
        return mealMapper.selectList(new LambdaQueryWrapper<MenuTemplateMeal>()
                .eq(MenuTemplateMeal::getTemplateId, templateId)
                .eq(MenuTemplateMeal::getDeleted, 0)
                .orderByAsc(MenuTemplateMeal::getSortOrder));
    }

    @Override
    public List<MenuTemplateMealItem> listMealItems(Long templateMealId) {
        return mealItemMapper.selectList(new LambdaQueryWrapper<MenuTemplateMealItem>()
                .eq(MenuTemplateMealItem::getTemplateMealId, templateMealId)
                .eq(MenuTemplateMealItem::getDeleted, 0)
                .orderByAsc(MenuTemplateMealItem::getSortOrder));
    }

    @Override
    public MenuTemplateDesignForm getDesignForm(Long templateId) {
        MenuTemplateDesignDto detail = getDesignDetail(templateId);
        MenuTemplateDesignForm form = new MenuTemplateDesignForm();
        form.setId(detail.getId());
        form.setName(detail.getName());
        form.setDescription(detail.getDescription());
        form.setThemeCode(detail.getThemeCode());
        for (MenuTemplateSectionDto section : detail.getSections()) {
            MenuTemplateSectionDesignForm sectionForm = new MenuTemplateSectionDesignForm();
            sectionForm.setId(section.getId());
            sectionForm.setSectionType(section.getSectionType());
            sectionForm.setTitle(section.getTitle());
            sectionForm.setSortOrder(section.getSortOrder());
            sectionForm.setEnabled(section.getEnabled());
            sectionForm.setAllowImage(section.getAllowImage());
            form.getSections().add(sectionForm);
        }
        for (MenuTemplateMealDto meal : detail.getMeals()) {
            MenuTemplateMealDesignForm mealForm = new MenuTemplateMealDesignForm();
            mealForm.setId(meal.getId());
            mealForm.setMealCode(meal.getMealCode());
            mealForm.setMealName(meal.getMealName());
            mealForm.setTimeLabel(meal.getTimeLabel());
            mealForm.setSortOrder(meal.getSortOrder());
            mealForm.setEnabled(meal.getEnabled());
            for (MenuTemplateMealItemDto item : meal.getItems()) {
                MenuTemplateMealItemDesignForm itemForm = new MenuTemplateMealItemDesignForm();
                itemForm.setId(item.getId());
                itemForm.setItemCode(item.getItemCode());
                itemForm.setItemName(item.getItemName());
                itemForm.setSortOrder(item.getSortOrder());
                itemForm.setEnabled(item.getEnabled());
                itemForm.setAllowImage(item.getAllowImage());
                mealForm.getItems().add(itemForm);
            }
            form.getMeals().add(mealForm);
        }
        return form;
    }

    @Override
    public MenuTemplateDesignDto getDesignDetail(Long templateId) {
        MenuTemplate template = requireTemplate(templateId);
        MenuTemplateDesignDto dto = new MenuTemplateDesignDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setThemeCode(template.getThemeCode());
        dto.setStatus(template.getStatus());
        dto.setIsDefault(template.getIsDefault());

        for (MenuTemplateSection section : listSections(templateId)) {
            MenuTemplateSectionDto sectionDto = new MenuTemplateSectionDto();
            sectionDto.setId(section.getId());
            sectionDto.setSectionType(section.getSectionType());
            sectionDto.setTitle(section.getTitle());
            sectionDto.setSortOrder(section.getSortOrder());
            sectionDto.setEnabled(toBoolean(section.getEnabled()));
            sectionDto.setAllowImage(toBoolean(section.getAllowImage()));
            sectionDto.setStyleConfigJson(section.getStyleConfigJson());
            dto.getSections().add(sectionDto);
        }

        for (MenuTemplateMeal meal : listMeals(templateId)) {
            MenuTemplateMealDto mealDto = new MenuTemplateMealDto();
            mealDto.setId(meal.getId());
            mealDto.setMealCode(meal.getMealCode());
            mealDto.setMealName(meal.getMealName());
            mealDto.setTimeLabel(meal.getTimeLabel());
            mealDto.setSortOrder(meal.getSortOrder());
            mealDto.setEnabled(toBoolean(meal.getEnabled()));
            for (MenuTemplateMealItem item : listMealItems(meal.getId())) {
                MenuTemplateMealItemDto itemDto = new MenuTemplateMealItemDto();
                itemDto.setId(item.getId());
                itemDto.setItemCode(item.getItemCode());
                itemDto.setItemName(item.getItemName());
                itemDto.setContentFormat(item.getContentFormat());
                itemDto.setSortOrder(item.getSortOrder());
                itemDto.setEnabled(toBoolean(item.getEnabled()));
                itemDto.setAllowImage(toBoolean(item.getAllowImage()));
                mealDto.getItems().add(itemDto);
            }
            dto.getMeals().add(mealDto);
        }
        return dto;
    }

    @Override
    public TemplatePreviewDto preview(Long templateId) {
        MenuTemplate template = requireTemplate(templateId);
        TemplatePreviewDto previewDto = new TemplatePreviewDto();
        previewDto.setTemplate(getDesignDetail(templateId));
        previewDto.setPreviewTitle(StringUtils.hasText(template.getTitleRule())
                ? template.getTitleRule().replace("{{customerName}}", "客户")
                : template.getName());
        return previewDto;
    }

    @Override
    @Transactional
    public MenuTemplate saveTemplate(TemplateSaveRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("TEMPLATE_NAME_EMPTY", "模板名称不能为空");
        }
        MenuTemplate template = request.getId() == null ? new MenuTemplate() : requireTemplate(request.getId());
        template.setName(request.getName().trim());
        template.setDescription(request.getDescription());
        template.setThemeCode(request.getThemeCode());
        template.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        template.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        if (template.getId() == null) {
            menuTemplateMapper.insert(template);
        } else {
            menuTemplateMapper.updateById(template);
        }
        return template;
    }

    @Override
    @Transactional
    public MenuTemplate copyTemplate(Long id, String name) {
        MenuTemplate source = requireTemplate(id);
        MenuTemplate target = new MenuTemplate();
        target.setName(StringUtils.hasText(name) ? name.trim() : source.getName() + "-副本");
        target.setDescription(source.getDescription());
        target.setThemeCode(source.getThemeCode());
        target.setCoverImagePath(source.getCoverImagePath());
        target.setTitleRule(source.getTitleRule());
        target.setStatus(source.getStatus());
        target.setIsDefault(0);
        menuTemplateMapper.insert(target);

        for (MenuTemplateSection section : listSections(id)) {
            MenuTemplateSection clone = new MenuTemplateSection();
            clone.setTemplateId(target.getId());
            clone.setSectionType(section.getSectionType());
            clone.setTitle(section.getTitle());
            clone.setSortOrder(section.getSortOrder());
            clone.setEnabled(section.getEnabled());
            clone.setStyleConfigJson(section.getStyleConfigJson());
            clone.setAllowImage(section.getAllowImage());
            sectionMapper.insert(clone);
        }

        for (MenuTemplateMeal meal : listMeals(id)) {
            MenuTemplateMeal mealClone = new MenuTemplateMeal();
            mealClone.setTemplateId(target.getId());
            mealClone.setMealCode(meal.getMealCode());
            mealClone.setMealName(meal.getMealName());
            mealClone.setTimeLabel(meal.getTimeLabel());
            mealClone.setSortOrder(meal.getSortOrder());
            mealClone.setEnabled(meal.getEnabled());
            mealMapper.insert(mealClone);

            for (MenuTemplateMealItem item : listMealItems(meal.getId())) {
                MenuTemplateMealItem itemClone = new MenuTemplateMealItem();
                itemClone.setTemplateMealId(mealClone.getId());
                itemClone.setItemCode(item.getItemCode());
                itemClone.setItemName(item.getItemName());
                itemClone.setContentFormat(item.getContentFormat());
                itemClone.setSortOrder(item.getSortOrder());
                itemClone.setEnabled(item.getEnabled());
                itemClone.setAllowImage(item.getAllowImage());
                mealItemMapper.insert(itemClone);
            }
        }
        return target;
    }

    @Override
    public void updateTemplateStatus(Long id, Integer status) {
        MenuTemplate template = requireTemplate(id);
        template.setStatus(status == null ? 1 : status);
        menuTemplateMapper.updateById(template);
    }

    @Override
    public void save(MenuTemplate template) {
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        if (template.getId() == null) {
            menuTemplateMapper.insert(template);
            return;
        }
        menuTemplateMapper.updateById(template);
    }

    @Override
    @Transactional
    public void saveDesign(MenuTemplateDesignForm form) {
        MenuTemplate template = requireTemplate(form.getId());
        template.setName(form.getName());
        template.setDescription(form.getDescription());
        template.setThemeCode(form.getThemeCode());
        menuTemplateMapper.updateById(template);

        for (MenuTemplateSectionDesignForm sectionForm : form.getSections()) {
            MenuTemplateSection section = sectionMapper.selectById(sectionForm.getId());
            section.setTitle(sectionForm.getTitle());
            section.setSortOrder(sectionForm.getSortOrder());
            section.setEnabled(Boolean.TRUE.equals(sectionForm.getEnabled()) ? 1 : 0);
            section.setAllowImage(Boolean.TRUE.equals(sectionForm.getAllowImage()) ? 1 : 0);
            sectionMapper.updateById(section);
        }

        for (MenuTemplateMealDesignForm mealForm : form.getMeals()) {
            MenuTemplateMeal meal = mealMapper.selectById(mealForm.getId());
            meal.setMealName(mealForm.getMealName());
            meal.setTimeLabel(mealForm.getTimeLabel());
            meal.setSortOrder(mealForm.getSortOrder());
            meal.setEnabled(Boolean.TRUE.equals(mealForm.getEnabled()) ? 1 : 0);
            mealMapper.updateById(meal);

            for (MenuTemplateMealItemDesignForm itemForm : mealForm.getItems()) {
                MenuTemplateMealItem item = mealItemMapper.selectById(itemForm.getId());
                item.setItemName(itemForm.getItemName());
                item.setSortOrder(itemForm.getSortOrder());
                item.setEnabled(Boolean.TRUE.equals(itemForm.getEnabled()) ? 1 : 0);
                item.setAllowImage(Boolean.TRUE.equals(itemForm.getAllowImage()) ? 1 : 0);
                mealItemMapper.updateById(item);
            }
        }
    }

    @Override
    @Transactional
    public MenuTemplateDesignDto saveDesign(TemplateDesignSaveRequest request) {
        MenuTemplate template = requireTemplate(request.getId());
        template.setName(request.getName().trim());
        template.setDescription(request.getDescription());
        template.setThemeCode(request.getThemeCode());
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        if (request.getIsDefault() != null) {
            template.setIsDefault(request.getIsDefault());
        }
        menuTemplateMapper.updateById(template);

        syncSections(template.getId(), request.getSections());
        syncMeals(template.getId(), request.getMeals());
        return getDesignDetail(template.getId());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireTemplate(id);
        menuTemplateMapper.deleteById(id);
        List<MenuTemplateSection> sections = listSections(id);
        for (MenuTemplateSection section : sections) {
            sectionMapper.deleteById(section.getId());
        }
        List<MenuTemplateMeal> meals = listMeals(id);
        for (MenuTemplateMeal meal : meals) {
            List<MenuTemplateMealItem> items = listMealItems(meal.getId());
            for (MenuTemplateMealItem item : items) {
                mealItemMapper.deleteById(item.getId());
            }
            mealMapper.deleteById(meal.getId());
        }
    }

    private void syncSections(Long templateId, List<TemplateSectionSaveRequest> sections) {
        List<MenuTemplateSection> existingSections = listSections(templateId);
        Set<Long> retainedIds = new HashSet<>();
        int sortOrder = 1;
        for (TemplateSectionSaveRequest sectionRequest : sections) {
            MenuTemplateSection section = sectionRequest.getId() == null
                    ? new MenuTemplateSection()
                    : requireSection(templateId, sectionRequest.getId());
            section.setTemplateId(templateId);
            section.setSectionType(sectionRequest.getSectionType().trim());
            section.setTitle(sectionRequest.getTitle().trim());
            section.setSortOrder(sortOrder++);
            section.setEnabled(toInt(sectionRequest.getEnabled(), true));
            section.setAllowImage(toInt(sectionRequest.getAllowImage(), false));
            section.setStyleConfigJson(sectionRequest.getStyleConfigJson());
            if (section.getId() == null) {
                sectionMapper.insert(section);
            } else {
                sectionMapper.updateById(section);
            }
            retainedIds.add(section.getId());
        }
        for (MenuTemplateSection section : existingSections) {
            if (!retainedIds.contains(section.getId())) {
                sectionMapper.deleteById(section.getId());
            }
        }
    }

    private void syncMeals(Long templateId, List<TemplateMealSaveRequest> meals) {
        List<MenuTemplateMeal> existingMeals = listMeals(templateId);
        Set<Long> retainedMealIds = new HashSet<>();
        int sortOrder = 1;
        for (TemplateMealSaveRequest mealRequest : meals) {
            MenuTemplateMeal meal = mealRequest.getId() == null
                    ? new MenuTemplateMeal()
                    : requireMeal(templateId, mealRequest.getId());
            meal.setTemplateId(templateId);
            meal.setMealCode(mealRequest.getMealCode().trim());
            meal.setMealName(mealRequest.getMealName().trim());
            meal.setTimeLabel(mealRequest.getTimeLabel());
            meal.setSortOrder(sortOrder++);
            meal.setEnabled(toInt(mealRequest.getEnabled(), true));
            if (meal.getId() == null) {
                mealMapper.insert(meal);
            } else {
                mealMapper.updateById(meal);
            }
            retainedMealIds.add(meal.getId());
            syncMealItems(meal.getId(), mealRequest.getItems());
        }
        for (MenuTemplateMeal meal : existingMeals) {
            if (!retainedMealIds.contains(meal.getId())) {
                deleteMealCascade(meal.getId());
            }
        }
    }

    private void syncMealItems(Long mealId, List<TemplateMealItemSaveRequest> items) {
        List<MenuTemplateMealItem> existingItems = listMealItems(mealId);
        Set<Long> retainedItemIds = new HashSet<>();
        int sortOrder = 1;
        for (TemplateMealItemSaveRequest itemRequest : items) {
            MenuTemplateMealItem item = itemRequest.getId() == null
                    ? new MenuTemplateMealItem()
                    : requireMealItem(mealId, itemRequest.getId());
            item.setTemplateMealId(mealId);
            item.setItemCode(itemRequest.getItemCode().trim());
            item.setItemName(itemRequest.getItemName().trim());
            item.setContentFormat(StringUtils.hasText(itemRequest.getContentFormat())
                    ? itemRequest.getContentFormat().trim() : "PLAIN_TEXT");
            item.setSortOrder(sortOrder++);
            item.setEnabled(toInt(itemRequest.getEnabled(), true));
            item.setAllowImage(toInt(itemRequest.getAllowImage(), false));
            if (item.getId() == null) {
                mealItemMapper.insert(item);
            } else {
                mealItemMapper.updateById(item);
            }
            retainedItemIds.add(item.getId());
        }
        for (MenuTemplateMealItem item : existingItems) {
            if (!retainedItemIds.contains(item.getId())) {
                mealItemMapper.deleteById(item.getId());
            }
        }
    }

    private void deleteMealCascade(Long mealId) {
        for (MenuTemplateMealItem item : listMealItems(mealId)) {
            mealItemMapper.deleteById(item.getId());
        }
        mealMapper.deleteById(mealId);
    }

    private MenuTemplateSection requireSection(Long templateId, Long sectionId) {
        MenuTemplateSection section = sectionMapper.selectById(sectionId);
        if (section == null || !templateId.equals(section.getTemplateId())) {
            throw new BizException("TEMPLATE_SECTION_NOT_FOUND", "未找到对应模板区块");
        }
        return section;
    }

    private MenuTemplateMeal requireMeal(Long templateId, Long mealId) {
        MenuTemplateMeal meal = mealMapper.selectById(mealId);
        if (meal == null || !templateId.equals(meal.getTemplateId())) {
            throw new BizException("TEMPLATE_MEAL_NOT_FOUND", "未找到对应模板餐次");
        }
        return meal;
    }

    private MenuTemplateMealItem requireMealItem(Long mealId, Long itemId) {
        MenuTemplateMealItem item = mealItemMapper.selectById(itemId);
        if (item == null || !mealId.equals(item.getTemplateMealId())) {
            throw new BizException("TEMPLATE_MEAL_ITEM_NOT_FOUND", "未找到对应模板字段");
        }
        return item;
    }

    private int toInt(Boolean value, boolean defaultValue) {
        boolean resolved = value != null ? value : defaultValue;
        return resolved ? 1 : 0;
    }

    private MenuTemplate requireTemplate(Long id) {
        MenuTemplate template = menuTemplateMapper.selectById(id);
        if (template == null || template.getDeleted() != null && template.getDeleted() == 1) {
            throw new BizException("TEMPLATE_NOT_FOUND", "未找到对应模板");
        }
        return template;
    }

    private TemplateSummaryDto toSummaryDto(MenuTemplate template) {
        TemplateSummaryDto dto = new TemplateSummaryDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setThemeCode(template.getThemeCode());
        dto.setStatus(template.getStatus());
        dto.setIsDefault(template.getIsDefault());
        return dto;
    }

    private Boolean toBoolean(Integer value) {
        return value != null && value == 1;
    }
}
