package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", authorities = "MENU_MANAGE")
class AdminFileApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void uploadImage_shouldReturnStoredPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "meal.png", "image/png", "png".getBytes());
        when(fileStorageService.store(file)).thenReturn("/uploads/2026/03/23/meal.png");

        mockMvc.perform(multipart("/api/admin/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.path").value("/uploads/2026/03/23/meal.png"));

        verify(fileStorageService).store(file);
    }

    @Test
    void uploadImage_withNonImageFile_shouldReturnBizError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "menu.txt", "text/plain", "demo".getBytes());

        mockMvc.perform(multipart("/api/admin/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FILE_TYPE_INVALID"));
    }
}
