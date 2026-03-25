package com.kfd.healthmenu.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiImageGenerateRequest {
    @NotBlank(message = "请先输入 AI 生图提示词")
    private String prompt;
}
