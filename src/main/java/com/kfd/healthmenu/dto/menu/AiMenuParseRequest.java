package com.kfd.healthmenu.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiMenuParseRequest {
    @NotBlank(message = "请先输入要解析的 AI 文本")
    private String sourceText;
}
