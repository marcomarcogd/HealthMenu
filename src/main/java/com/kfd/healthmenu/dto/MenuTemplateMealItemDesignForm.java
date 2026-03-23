package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class MenuTemplateMealItemDesignForm {
    private Long id;
    private String itemCode;
    private String itemName;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowImage;
}
