package com.kfd.healthmenu.dto.template;

import lombok.Data;

@Data
public class MenuTemplateSectionDto {
    private Long id;
    private String sectionType;
    private String title;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowImage;
    private String styleConfigJson;
}
