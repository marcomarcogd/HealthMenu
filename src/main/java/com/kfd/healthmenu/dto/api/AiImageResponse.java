package com.kfd.healthmenu.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiImageResponse {
    private String path;
    private String prompt;
}
