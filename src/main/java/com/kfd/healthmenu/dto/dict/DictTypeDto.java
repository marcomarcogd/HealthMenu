package com.kfd.healthmenu.dto.dict;

import lombok.Data;

@Data
public class DictTypeDto {
    private Long id;
    private String typeCode;
    private String typeName;
    private String description;
    private Integer status;
}
