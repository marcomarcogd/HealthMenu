package com.kfd.healthmenu.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateMealItemSaveRequest {
    private Long id;
    @NotBlank
    private String itemCode;
    @NotBlank
    private String itemName;
    private String contentFormat;
    private Boolean enabled;
    private Boolean allowImage;
}
