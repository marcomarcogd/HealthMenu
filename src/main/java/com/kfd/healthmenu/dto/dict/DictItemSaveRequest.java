package com.kfd.healthmenu.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictItemSaveRequest {
    private Long id;

    @NotNull(message = "字典类型不能为空")
    private Long dictTypeId;

    @NotBlank(message = "字典项编码不能为空")
    private String itemCode;

    @NotBlank(message = "字典项名称不能为空")
    private String itemLabel;

    @NotBlank(message = "字典项值不能为空")
    private String itemValue;

    private Integer sortOrder;
    private Integer isSystem;
    private Integer status;
}
