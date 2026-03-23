package com.kfd.healthmenu.dto.template;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTemplateDesignDto {
    private Long id;
    private String name;
    private String description;
    private String themeCode;
    private Integer status;
    private Integer isDefault;
    private List<MenuTemplateSectionDto> sections = new ArrayList<>();
    private List<MenuTemplateMealDto> meals = new ArrayList<>();
}
