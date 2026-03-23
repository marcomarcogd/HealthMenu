package com.kfd.healthmenu.dto.dict;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictTypeSaveRequest {
    private Long id;

    @NotBlank(message = "字典类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "字典类型名称不能为空")
    private String typeName;

    private String description;
    private Integer status;
}
