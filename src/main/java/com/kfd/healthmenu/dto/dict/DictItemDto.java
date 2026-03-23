package com.kfd.healthmenu.dto.dict;

import lombok.Data;

@Data
public class DictItemDto {
    private Long id;
    private Long dictTypeId;
    private String itemCode;
    private String itemLabel;
    private String itemValue;
    private Integer sortOrder;
    private Integer isSystem;
    private Integer status;
}
