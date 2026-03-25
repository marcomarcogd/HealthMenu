package com.kfd.healthmenu.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateDesignSaveRequest {
    private Long id;
    @NotBlank(message = "模板名称不能为空")
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
