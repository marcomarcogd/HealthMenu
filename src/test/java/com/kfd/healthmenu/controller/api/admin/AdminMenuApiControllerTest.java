package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.CustomerMenuMealForm;
import com.kfd.healthmenu.dto.CustomerMenuMealItemForm;
import com.kfd.healthmenu.dto.api.PageResult;
import com.kfd.healthmenu.dto.menu.CustomerMenuSummaryDto;
import com.kfd.healthmenu.service.CustomerMenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminMenuApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerMenuService customerMenuService;

    @Test
    void list_shouldReturnPagedSummaries() throws Exception {
        CustomerMenuSummaryDto summary = new CustomerMenuSummaryDto();
        summary.setId(7001L);
        summary.setTitle("午餐恢复日");
        summary.setCustomerName("张三");
        summary.setStatus("DRAFT");
        summary.setStatusLabel("草稿");
        when(customerMenuService.listSummaries(argThat(query ->
                query != null
                        && "午餐".equals(query.getKeyword())
                        && 2001L == query.getCustomerId()
                        && "DRAFT".equals(query.getStatus())
                        && "titleAsc".equals(query.getSort())
                        && query.getPage() == 2L
                        && query.getPageSize() == 5L
        ))).thenReturn(new PageResult<>(List.of(summary), 6L, 2L, 5L));

        mockMvc.perform(get("/api/admin/menus")
                        .param("keyword", "午餐")
                        .param("customerId", "2001")
                        .param("status", "DRAFT")
                        .param("sort", "titleAsc")
                        .param("page", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records[0].title").value("午餐恢复日"))
                .andExpect(jsonPath("$.data.records[0].customerName").value("张三"))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    @Test
    void init_withoutAi_shouldReturnTemplateBasedForm() throws Exception {
        when(customerMenuService.buildCreateForm(2001L, 1001L)).thenReturn(buildBaseForm("普通初始化标题"));

        mockMvc.perform(post("/api/admin/menus/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"customerId\": 2001,
                                  \"templateId\": 1001,
                                  \"useAi\": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("普通初始化标题"))
                .andExpect(jsonPath("$.data.meals[0].mealCode").value("breakfast"));
    }

    @Test
    void init_withAi_shouldReturnAiMergedForm() throws Exception {
        AiImportResultDto aiResult = new AiImportResultDto();
        aiResult.setTitle("AI 识别标题");
        aiResult.setWeekIndex(2);
        when(customerMenuService.parseAiMenuText(anyString())).thenReturn(aiResult);
        when(customerMenuService.buildCreateFormFromAi(2001L, 1001L, aiResult)).thenReturn(buildBaseForm("AI 识别标题"));

        mockMvc.perform(post("/api/admin/menus/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"customerId\": 2001,
                                  \"templateId\": 1001,
                                  \"useAi\": true,
                                  \"sourceText\": \"早餐：鸡蛋\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("AI 识别标题"));
    }

    @Test
    void detail_shouldReturnExistingMenuForm() throws Exception {
        CustomerMenuForm form = buildBaseForm("历史餐单");
        form.setId(7001L);
        when(customerMenuService.getFormById(7001L)).thenReturn(form);

        mockMvc.perform(get("/api/admin/menus/7001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("7001"))
                .andExpect(jsonPath("$.data.title").value("历史餐单"));
    }

    @Test
    void save_shouldReturnPersistedId() throws Exception {
        when(customerMenuService.saveMenu(org.mockito.ArgumentMatchers.any(CustomerMenuForm.class))).thenReturn(9001L);

        mockMvc.perform(post("/api/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"customerId\": 2001,
                                  \"templateId\": 1001,
                                  \"menuDate\": \"2026-03-22\",
                                  \"weekIndex\": 3,
                                  \"title\": \"保存测试餐单\",
                                  \"showWeeklyTip\": true,
                                  \"showSwapGuide\": false,
                                  \"status\": \"PUBLISHED\",
                                  \"sections\": [
                                    {
                                      \"sectionType\": \"WEEKLY_TIP\",
                                      \"title\": \"每周提示\",
                                      \"content\": \"多喝水\",
                                      \"sortOrder\": 1
                                    }
                                  ],
                                  \"meals\": [
                                    {
                                      \"mealCode\": \"breakfast\",
                                      \"mealName\": \"早餐\",
                                      \"timeLabel\": \"早餐时间\",
                                      \"mealTime\": \"07:30\",
                                      \"sortOrder\": 1,
                                      \"items\": [
                                        {
                                          \"itemCode\": \"protein\",
                                          \"itemName\": \"蛋白\",
                                          \"itemValue\": \"鸡蛋 2 个\",
                                          \"sortOrder\": 1
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("餐单保存成功"))
                .andExpect(jsonPath("$.data.id").value("9001"));
    }

    @Test
    void save_withoutMenuDate_shouldReturnValidationResponse() throws Exception {
        mockMvc.perform(post("/api/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"customerId\": 2001,
                                  \"templateId\": 1001,
                                  \"title\": \"缺日期餐单\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请选择餐单日期"));
    }

    @Test
    void publish_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/menus/7001/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("餐单已发布"));

        verify(customerMenuService).publishMenu(7001L);
    }

    @Test
    void batchPublish_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/menus/batch/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [7001, 7002]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("已批量发布餐单"));

        verify(customerMenuService).publishMenus(List.of(7001L, 7002L));
    }

    @Test
    void exportExcel_shouldDelegateToService() throws Exception {
        mockMvc.perform(get("/api/admin/menus/7001/export/excel"))
                .andExpect(status().isOk());

        verify(customerMenuService).exportMenuExcel(org.mockito.ArgumentMatchers.eq(7001L), any(HttpServletResponse.class));
    }

    @Test
    void batchExportExcel_shouldDelegateToService() throws Exception {
        mockMvc.perform(post("/api/admin/menus/batch/export/excel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [7001, 7002]
                                }
                                """))
                .andExpect(status().isOk());

        verify(customerMenuService).exportMenusExcel(org.mockito.ArgumentMatchers.eq(List.of(7001L, 7002L)), any(HttpServletResponse.class));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/admin/menus/7001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("餐单已删除"));

        verify(customerMenuService).deleteById(7001L);
    }

    @Test
    void parse_shouldReturnAiSummary() throws Exception {
        AiImportResultDto aiResult = new AiImportResultDto();
        aiResult.setTitle("识别结果");
        aiResult.setWeekIndex(6);
        when(customerMenuService.parseAiMenuText(anyString())).thenReturn(aiResult);

        mockMvc.perform(post("/api/admin/menus/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"sourceText\": \"午餐：米饭\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("识别结果"))
                .andExpect(jsonPath("$.data.weekIndex").value(6));
    }

    @Test
    void parse_withoutSourceText_shouldReturnValidationResponse() throws Exception {
        mockMvc.perform(post("/api/admin/menus/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"sourceText\": \"\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请先输入要解析的 AI 文本"));
    }

    @Test
    void generateImage_withoutPrompt_shouldReturnValidationResponse() throws Exception {
        mockMvc.perform(post("/api/admin/menus/ai/generate-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请先输入 AI 生图提示词"));
    }

    private CustomerMenuForm buildBaseForm(String title) {
        CustomerMenuForm form = new CustomerMenuForm();
        form.setCustomerId(2001L);
        form.setTemplateId(1001L);
        form.setMenuDate(LocalDate.of(2026, 3, 22));
        form.setTitle(title);

        CustomerMenuMealItemForm item = new CustomerMenuMealItemForm();
        item.setItemCode("protein");
        item.setItemName("蛋白");
        item.setItemValue("鸡蛋");
        item.setSortOrder(1);

        CustomerMenuMealForm meal = new CustomerMenuMealForm();
        meal.setMealCode("breakfast");
        meal.setMealName("早餐");
        meal.setItems(List.of(item));
        meal.setSortOrder(1);
        form.setMeals(List.of(meal));
        return form;
    }
}
