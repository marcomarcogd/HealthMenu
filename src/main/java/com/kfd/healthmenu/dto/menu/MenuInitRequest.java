package com.kfd.healthmenu.dto.menu;

import com.kfd.healthmenu.dto.CustomerMenuForm;
import lombok.Data;

@Data
public class MenuInitRequest {
    private Long customerId;
    private Long templateId;
    private String sourceText;
    private Boolean useAi;
}
