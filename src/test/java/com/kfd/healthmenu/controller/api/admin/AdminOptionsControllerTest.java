package com.kfd.healthmenu.controller.api.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", authorities = "OPTIONS_READ")
class AdminOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOptions_shouldReturnCustomersTemplatesAndStatuses() throws Exception {
        mockMvc.perform(get("/api/admin/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customers.length()").value(2))
                .andExpect(jsonPath("$.data.customers[0].label").isNotEmpty())
                .andExpect(jsonPath("$.data.templates").isArray())
                .andExpect(jsonPath("$.data.recordStatuses[0].value").value("DRAFT"))
                .andExpect(jsonPath("$.data.recordStatuses[1].value").value("PUBLISHED"));
    }
}
