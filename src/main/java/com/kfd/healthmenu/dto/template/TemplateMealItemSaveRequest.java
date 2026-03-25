package com.kfd.healthmenu.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateMealItemSaveRequest {
    private Long id;
    @NotBlank(message = "字段编码不能为空")
    private String itemCode;
    @NotBlank(message = "字段名称不能为空")
    private String itemName;
    private String contentFormat;
    private Boolean enabled;
    private Boolean allowImage;
}
