package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.MenuTemplateDesignForm;
import com.kfd.healthmenu.dto.template.MenuTemplateDesignDto;
import com.kfd.healthmenu.dto.template.TemplateDesignSaveRequest;
import com.kfd.healthmenu.dto.template.TemplatePreviewDto;
import com.kfd.healthmenu.dto.template.TemplateSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateSummaryDto;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.entity.MenuTemplateMeal;
import com.kfd.healthmenu.entity.MenuTemplateMealItem;
import com.kfd.healthmenu.entity.MenuTemplateSection;

import java.util.List;

public interface TemplateService {
    List<MenuTemplate> listAll();

    List<TemplateSummaryDto> listSummaries();

    MenuTemplate getById(Long id);

    List<MenuTemplateSection> listSections(Long templateId);

    List<MenuTemplateMeal> listMeals(Long templateId);

    List<MenuTemplateMealItem> listMealItems(Long templateMealId);

    MenuTemplateDesignForm getDesignForm(Long templateId);

    MenuTemplateDesignDto getDesignDetail(Long templateId);

    TemplatePreviewDto preview(Long templateId);

    MenuTemplate saveTemplate(TemplateSaveRequest request);

    MenuTemplate copyTemplate(Long id, String name);

    void updateTemplateStatus(Long id, Integer status);

    void save(MenuTemplate template);

    void saveDesign(MenuTemplateDesignForm form);

    MenuTemplateDesignDto saveDesign(TemplateDesignSaveRequest request);

    void deleteById(Long id);
}
