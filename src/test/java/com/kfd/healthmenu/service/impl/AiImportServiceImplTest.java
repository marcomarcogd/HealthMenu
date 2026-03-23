package com.kfd.healthmenu.service.impl;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;
import com.kfd.healthmenu.service.AiImportService;
import com.kfd.healthmenu.service.CozeWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AiImportServiceImplTest {

    @Autowired
    private AiImportService aiImportService;

    @MockBean
    private CozeWorkflowService cozeWorkflowService;

    @Test
    void parseMenuText_shouldNotInjectSampleMealsWhenFallbacking() {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        response.setRawResponse("mock fallback");
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        AiImportResultDto result = aiImportService.parseMenuText("这周注意清淡饮食");

        assertThat(result.getParseMode()).isIn("HEURISTIC", "FALLBACK");
        assertThat(result.getMeals()).isEmpty();
        assertThat(result.getParseMessage()).doesNotContain("默认结构");
    }

    @Test
    void parseMenuText_shouldKeepHeuristicMealsWhenTextContainsMealLines() {
        CozeWorkflowResponse response = new CozeWorkflowResponse();
        response.setSuccess(false);
        response.setRawResponse("mock heuristic");
        when(cozeWorkflowService.execute(any())).thenReturn(response);

        AiImportResultDto result = aiImportService.parseMenuText("""
                标题：减脂计划
                第3周
                早餐：燕麦 40g；蛋白：鸡蛋 2 个
                晚餐：玉米 1 根
                """);

        assertThat(result.getParseMode()).isEqualTo("HEURISTIC");
        assertThat(result.getMeals()).hasSize(2);
        assertThat(result.getMeals().get(0).getMealCode()).isEqualTo("breakfast");
        assertThat(result.getMeals().get(0).getItems()).isNotEmpty();
    }
}
