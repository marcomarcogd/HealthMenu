package com.kfd.healthmenu.dto.template;

import lombok.Data;

@Data
public class MenuTemplateMealItemDto {
    private Long id;
    private String itemCode;
    private String itemName;
    private String contentFormat;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowImage;
}
