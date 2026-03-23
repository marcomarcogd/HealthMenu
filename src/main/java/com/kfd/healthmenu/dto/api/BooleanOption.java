package com.kfd.healthmenu.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BooleanOption {
    private String label;
    private Boolean value;
}
