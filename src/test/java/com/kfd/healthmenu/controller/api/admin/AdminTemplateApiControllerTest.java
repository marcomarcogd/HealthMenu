package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.template.MenuTemplateDesignDto;
import com.kfd.healthmenu.dto.template.MenuTemplateMealDto;
import com.kfd.healthmenu.dto.template.MenuTemplateMealItemDto;
import com.kfd.healthmenu.dto.template.MenuTemplateSectionDto;
import com.kfd.healthmenu.dto.template.TemplateDesignSaveRequest;
import com.kfd.healthmenu.service.TemplateService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminTemplateApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Test
    void detail_shouldReturnTemplateStructure() throws Exception {
        when(templateService.getDesignDetail(1001L)).thenReturn(buildTemplateDesign());

        mockMvc.perform(get("/api/admin/templates/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("1001"))
                .andExpect(jsonPath("$.data.sections[0].title").value("专属标题"))
                .andExpect(jsonPath("$.data.meals[0].mealCode").value("breakfast"));
    }

    @Test
    void saveDesign_shouldPersistAndEchoSortedStructure() throws Exception {
        when(templateService.saveDesign(argThat((TemplateDesignSaveRequest request) -> request != null && request.getId() == 1001L)))
                .thenReturn(buildTemplateDesign());

        String requestBody = """
                {
                  "name": "模板接口测试",
                  "description": "controller-save",
                  "themeCode": "fresh",
                  "status": 1,
                  "isDefault": 0,
                  "sections": [
                    {
                      "id": 3003,
                      "sectionType": "WEEKLY_TIP",
                      "title": "每周提示改版",
                      "enabled": true,
                      "allowImage": false,
                      "styleConfigJson": "{\\"style\\":\\"tip\\"}"
                    },
                    {
                      "sectionType": "REMARK",
                      "title": "备注说明",
                      "enabled": true,
                      "allowImage": true,
                      "styleConfigJson": "{\\"style\\":\\"remark\\"}"
                    }
                  ],
                  "meals": [
                    {
                      "id": 4001,
                      "mealCode": "breakfast",
                      "mealName": "早餐新版",
                      "timeLabel": "07:00",
                      "enabled": true,
                      "items": [
                        {
                          "id": 5002,
                          "itemCode": "protein",
                          "itemName": "蛋白质",
                          "contentFormat": "PLAIN_TEXT",
                          "enabled": true,
                          "allowImage": false
                        },
                        {
                          "itemCode": "drink",
                          "itemName": "饮品",
                          "contentFormat": "RICH_TEXT",
                          "enabled": true,
                          "allowImage": true
                        }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/templates/1001/design")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("模板结构保存成功"))
                .andExpect(jsonPath("$.data.id").value("1001"))
                .andExpect(jsonPath("$.data.sections[0].title").value("专属标题"));
    }

    @Test
    void saveDesign_withoutName_shouldReturnValidationResponse() throws Exception {
        mockMvc.perform(post("/api/admin/templates/1001/design")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sections": [],
                                  "meals": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private MenuTemplateDesignDto buildTemplateDesign() {
        MenuTemplateSectionDto section = new MenuTemplateSectionDto();
        section.setId(3001L);
        section.setSectionType("WEEKLY_TIP");
        section.setTitle("专属标题");
        section.setSortOrder(1);
        section.setEnabled(true);
        section.setAllowImage(false);

        MenuTemplateMealItemDto item = new MenuTemplateMealItemDto();
        item.setId(5001L);
        item.setItemCode("protein");
        item.setItemName("蛋白质");
        item.setContentFormat("PLAIN_TEXT");
        item.setSortOrder(1);
        item.setEnabled(true);
        item.setAllowImage(false);

        MenuTemplateMealDto meal = new MenuTemplateMealDto();
        meal.setId(4001L);
        meal.setMealCode("breakfast");
        meal.setMealName("早餐");
        meal.setTimeLabel("07:00");
        meal.setSortOrder(1);
        meal.setEnabled(true);
        meal.setItems(List.of(item));

        MenuTemplateDesignDto dto = new MenuTemplateDesignDto();
        dto.setId(1001L);
        dto.setName("标准模板");
        dto.setDescription("mock-template");
        dto.setThemeCode("fresh");
        dto.setStatus(1);
        dto.setIsDefault(0);
        dto.setSections(List.of(section));
        dto.setMeals(List.of(meal));
        return dto;
    }
}
