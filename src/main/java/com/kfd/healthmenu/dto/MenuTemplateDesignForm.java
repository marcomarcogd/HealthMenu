package com.kfd.healthmenu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTemplateDesignForm {
    @NotNull
    private Long id;
    private String name;
    private String description;
    private String themeCode;
    private List<MenuTemplateSectionDesignForm> sections = new ArrayList<>();
    private List<MenuTemplateMealDesignForm> meals = new ArrayList<>();
}
