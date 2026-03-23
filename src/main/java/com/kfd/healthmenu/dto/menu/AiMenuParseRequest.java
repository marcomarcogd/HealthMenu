package com.kfd.healthmenu.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiMenuParseRequest {
    @NotBlank
    private String sourceText;
}
