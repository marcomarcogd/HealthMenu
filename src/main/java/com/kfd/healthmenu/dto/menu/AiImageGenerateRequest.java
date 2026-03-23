package com.kfd.healthmenu.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiImageGenerateRequest {
    @NotBlank
    private String prompt;
}
