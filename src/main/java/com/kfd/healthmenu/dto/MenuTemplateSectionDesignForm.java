package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class MenuTemplateSectionDesignForm {
    private Long id;
    private String sectionType;
    private String title;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowImage;
}
