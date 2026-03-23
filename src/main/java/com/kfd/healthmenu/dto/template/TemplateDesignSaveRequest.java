package com.kfd.healthmenu.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateDesignSaveRequest {
    @NotNull
    private Long id;
    @NotBlank
    private String name;
    private String description;
    private String themeCode;
    private Integer status;
    private Integer isDefault;
    @Valid
    private List<TemplateSectionSaveRequest> sections = new ArrayList<>();
    @Valid
    private List<TemplateMealSaveRequest> meals = new ArrayList<>();
}
