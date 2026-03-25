package com.kfd.healthmenu.controller.api.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", authorities = "ROLE_MANAGE")
class AdminRoleApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_shouldReturnBuiltInRoles() throws Exception {
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleCode").exists());
    }

    @Test
    void save_shouldCreateCustomRole() throws Exception {
        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "CONTENT_EDITOR",
                                  "roleName": "内容编辑",
                                  "permissionCodes": ["DASHBOARD_VIEW", "MENU_MANAGE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("CONTENT_EDITOR"))
                .andExpect(jsonPath("$.data.permissionCodes.length()").value(2));
    }

    @Test
    void permissions_shouldReturnAvailablePermissionOptions() throws Exception {
        mockMvc.perform(get("/api/admin/roles/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].label").exists());
    }

    @Test
    void delete_shouldRejectSystemRole() throws Exception {
        String body = mockMvc.perform(get("/api/admin/roles"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        java.util.List<String> roleIds = com.jayway.jsonpath.JsonPath.read(body, "$.data[?(@.roleCode=='ADMIN')].id");
        String roleId = roleIds.get(0);

        mockMvc.perform(delete("/api/admin/roles/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SYSTEM_ROLE_DELETE_FORBIDDEN"));
    }

    @Test
    void save_shouldKeepAdminManagePermissions() throws Exception {
        String body = mockMvc.perform(get("/api/admin/roles"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        java.util.List<String> roleIds = com.jayway.jsonpath.JsonPath.read(body, "$.data[?(@.roleCode=='ADMIN')].id");
        String roleId = roleIds.get(0);

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %s,
                                  "roleCode": "ADMIN",
                                  "roleName": "管理员",
                                  "permissionCodes": ["DASHBOARD_VIEW", "MENU_MANAGE"]
                                }
                                """.formatted(roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN_ROLE_PERMISSION_REQUIRED"));
    }
}
