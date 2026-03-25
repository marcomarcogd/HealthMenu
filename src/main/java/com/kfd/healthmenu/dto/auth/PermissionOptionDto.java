package com.kfd.healthmenu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PermissionOptionDto {
    private String code;
    private String label;
    private String groupLabel;
    private String description;
}
