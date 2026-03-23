package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class CozeWorkflowRequest {
    private String workflowCode;
    private String sourceText;
    private String sourceImagePath;
    private String sourceImageUrl;
    private String templateHint;
    private String customerName;
    private String prompt;
    private String sceneType;
    private String styleHint;
}
