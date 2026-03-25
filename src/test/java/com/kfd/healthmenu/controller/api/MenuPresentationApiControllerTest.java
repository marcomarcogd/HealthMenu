package com.kfd.healthmenu.controller.api;

import com.kfd.healthmenu.common.RecordStatus;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.mapper.CustomerMenuMapper;
import com.kfd.healthmenu.service.CustomerMenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MenuPresentationApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerMenuService customerMenuService;

    @MockBean
    private CustomerMenuMapper customerMenuMapper;

    @Test
    void view_shouldReturnPresentationPayload() throws Exception {
        CustomerMenu menu = new CustomerMenu();
        menu.setId(7001L);
        menu.setStatus(RecordStatus.PUBLISHED.name());

        CustomerMenuForm form = new CustomerMenuForm();
        form.setId(7001L);
        form.setTitle("展示餐单");
        form.setMenuDate(LocalDate.of(2026, 3, 23));

        when(customerMenuMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(menu);
        when(customerMenuService.getFormById(7001L)).thenReturn(form);

        mockMvc.perform(get("/api/public/menus/7001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareMode").value(false))
                .andExpect(jsonPath("$.data.menuForm.id").value("7001"))
                .andExpect(jsonPath("$.data.menuForm.title").value("展示餐单"));
    }

    @Test
    void view_shouldRejectDraftMenuWhenNotLoggedIn() throws Exception {
        CustomerMenu menu = new CustomerMenu();
        menu.setId(7001L);
        menu.setStatus(RecordStatus.DRAFT.name());

        when(customerMenuMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(menu);

        mockMvc.perform(get("/api/public/menus/7001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MENU_LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("餐单尚未发布，请先登录后台后查看预览"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void view_shouldAllowDraftMenuWhenLoggedIn() throws Exception {
        CustomerMenu menu = new CustomerMenu();
        menu.setId(7001L);
        menu.setStatus(RecordStatus.DRAFT.name());

        CustomerMenuForm form = new CustomerMenuForm();
        form.setId(7001L);
        form.setTitle("后台预览餐单");

        when(customerMenuMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(menu);
        when(customerMenuService.getFormById(7001L)).thenReturn(form);

        mockMvc.perform(get("/api/public/menus/7001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.menuForm.title").value("后台预览餐单"));
    }

    @Test
    void share_shouldReturnShareModePayload() throws Exception {
        CustomerMenu menu = new CustomerMenu();
        menu.setId(7001L);
        menu.setStatus(RecordStatus.PUBLISHED.name());

        CustomerMenuForm form = new CustomerMenuForm();
        form.setId(7001L);
        form.setTitle("分享餐单");

        when(customerMenuMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(menu);
        when(customerMenuService.getFormById(7001L)).thenReturn(form);

        mockMvc.perform(get("/api/public/menus/share/share-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareMode").value(true))
                .andExpect(jsonPath("$.data.menuForm.title").value("分享餐单"));
    }

    @Test
    void share_shouldRejectDraftMenu() throws Exception {
        when(customerMenuMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        mockMvc.perform(get("/api/public/menus/share/share-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MENU_NOT_PUBLISHED"))
                .andExpect(jsonPath("$.message").value("餐单尚未发布，暂不能通过分享链接查看"));
    }
}
