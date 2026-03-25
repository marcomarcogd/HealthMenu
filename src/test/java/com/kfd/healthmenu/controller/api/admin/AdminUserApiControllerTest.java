package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.common.UserRole;
import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.mapper.SysUserMapper;
import com.kfd.healthmenu.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", authorities = "USER_MANAGE")
class AdminUserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void list_shouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").exists());
    }

    @Test
    void save_shouldCreateUser() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "coach01",
                                  "displayName": "张健管",
                                  "roleCode": "HEALTH_MANAGER",
                                  "status": 1,
                                  "password": "Coach@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("coach01"))
                .andExpect(jsonPath("$.data.roleCode").value("HEALTH_MANAGER"));
    }

    @Test
    void resetPassword_shouldSucceed() throws Exception {
        SysUser user = createUser("reset01", UserRole.HEALTH_MANAGER);

        mockMvc.perform(post("/api/admin/users/{id}/reset-password", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "NewPass@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码已重置"));
    }

    @Test
    void delete_shouldRejectDeletingSelf() throws Exception {
        SysUser current = createUser("current-admin", UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/users/{id}", current.getId())
                        .with(user(new AuthenticatedUser(
                                current.getId(),
                                current.getUsername(),
                                current.getPassword(),
                                current.getDisplayName(),
                                current.getRoleCode(),
                                "管理员",
                                current.getStatus(),
                                java.util.List.of("USER_MANAGE", "ROLE_MANAGE")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DELETE_SELF_FORBIDDEN"));
    }

    @Test
    void audits_shouldRecordCreateResetAndDelete() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "audit01",
                                  "displayName": "审计账号",
                                  "roleCode": "HEALTH_MANAGER",
                                  "status": 1,
                                  "password": "Audit@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        SysUser target = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "audit01")
                .last("limit 1"));

        mockMvc.perform(post("/api/admin/users/{id}/reset-password", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Audit@654321"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/users/audits").param("targetUserId", String.valueOf(target.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].actionCode").value("DELETE"))
                .andExpect(jsonPath("$.data[1].actionCode").value("RESET_PASSWORD"))
                .andExpect(jsonPath("$.data[2].actionCode").value("CREATE"))
                .andExpect(jsonPath("$.data[0].operatorUsername").value("admin"));
    }

    private SysUser createUser(String username, UserRole role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("Temp@123456"));
        user.setDisplayName(username);
        user.setRoleCode(role.getCode());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user;
    }
}
