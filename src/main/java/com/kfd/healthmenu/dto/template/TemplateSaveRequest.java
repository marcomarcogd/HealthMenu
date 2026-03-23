package com.kfd.healthmenu.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateSaveRequest {
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private String description;
    private String themeCode;
    private Integer status;
    private Integer isDefault;
}
