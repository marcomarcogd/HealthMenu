package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class CozeWorkflowResponse {
    private Boolean success;
    private String rawResponse;
    private String parsedText;
    private String imageUrl;
    private String taskId;
    private String prompt;
    private String errorMessage;
}
