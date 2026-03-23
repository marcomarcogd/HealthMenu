package com.kfd.healthmenu.dto.template;

import lombok.Data;

@Data
public class TemplateSummaryDto {
    private Long id;
    private String name;
    private String description;
    private String themeCode;
    private Integer status;
    private Integer isDefault;
}
