package com.kfd.healthmenu.service.impl;

import com.kfd.healthmenu.dto.CozeWorkflowRequest;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CozeWorkflowServiceImplTest {

    @Test
    void buildPayload_shouldTreatConfiguredImageWorkflowAsImageRequest() {
        CozeWorkflowServiceImpl service = new CozeWorkflowServiceImpl();
        ReflectionTestUtils.setField(service, "imageWorkflowCode", "meal-poster");

        CozeWorkflowRequest request = new CozeWorkflowRequest();
        request.setWorkflowCode("meal-poster");
        request.setPrompt("一盘减脂午餐");
        request.setSceneType("meal_item");
        request.setStyleHint("营养师海报风");

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(service, "buildPayload", request);

        assertThat(payload)
                .containsEntry("prompt", "一盘减脂午餐")
                .containsEntry("sceneType", "meal_item")
                .containsEntry("styleHint", "营养师海报风")
                .doesNotContainKey("sourceText");
    }

    @Test
    void buildPayload_shouldUseBlankStringsForOptionalTextFields() {
        CozeWorkflowServiceImpl service = new CozeWorkflowServiceImpl();
        CozeWorkflowRequest request = new CozeWorkflowRequest();
        request.setWorkflowCode("text");
        request.setSourceText("早餐：燕麦");

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(service, "buildPayload", request);

        assertThat(payload)
                .containsEntry("sourceText", "早餐：燕麦")
                .containsEntry("sourceImageUrl", "")
                .containsEntry("templateHint", "")
                .containsEntry("customerName", "");
    }

    @Test
    void parseResponse_shouldExtractNestedImageUrlAndSuccessFlag() {
        CozeWorkflowServiceImpl service = new CozeWorkflowServiceImpl();
        CozeWorkflowResponse response = new CozeWorkflowResponse();

        ReflectionTestUtils.invokeMethod(service, "parseResponse", """
                {
                  "success": true,
                  "data": {
                    "images": [
                      { "url": "https://example.com/generated.jpeg" }
                    ],
                    "task_id": "task-001"
                  }
                }
                """, response);

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/generated.jpeg");
        assertThat(response.getTaskId()).isEqualTo("task-001");
    }

    @Test
    void parseResponse_shouldExtractErrorMessageFromJsonBody() {
        CozeWorkflowServiceImpl service = new CozeWorkflowServiceImpl();
        CozeWorkflowResponse response = new CozeWorkflowResponse();

        ReflectionTestUtils.invokeMethod(service, "parseResponse", """
                {
                  "msg": "Access token is invalid. Please provide a valid token and try again"
                }
                """, response);

        assertThat(response.getErrorMessage()).contains("Access token is invalid");
    }
}
