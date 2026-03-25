package com.kfd.healthmenu.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.UserRole;
import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AccountBootstrapConfig {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.bootstrap-admin-username:admin}")
    private String bootstrapAdminUsername;

    @Value("${app.auth.bootstrap-admin-password:Admin@123456}")
    private String bootstrapAdminPassword;

    @Value("${app.auth.bootstrap-admin-display-name:系统管理员}")
    private String bootstrapAdminDisplayName;

    @Value("${app.auth.bootstrap-manager-username:manager}")
    private String bootstrapManagerUsername;

    @Value("${app.auth.bootstrap-manager-password:Manager@123456}")
    private String bootstrapManagerPassword;

    @Value("${app.auth.bootstrap-manager-display-name:健管师}")
    private String bootstrapManagerDisplayName;

    @Bean
    public ApplicationRunner accountBootstrapRunner() {
        return args -> {
            createUserIfMissing(bootstrapAdminUsername, bootstrapAdminPassword, bootstrapAdminDisplayName, UserRole.ADMIN);
            createUserIfMissing(bootstrapManagerUsername, bootstrapManagerPassword, bootstrapManagerDisplayName, UserRole.HEALTH_MANAGER);
        };
    }

    private void createUserIfMissing(String username, String password, String displayName, UserRole role) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(displayName)) {
            return;
        }
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .eq(SysUser::getDeleted, 0));
        if (count != null && count > 0) {
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password.trim()));
        user.setDisplayName(displayName.trim());
        user.setRoleCode(role.getCode());
        user.setStatus(1);
        sysUserMapper.insert(user);
        log.warn("已自动创建默认账号：{}（{}），请尽快登录后台修改默认密码", user.getUsername(), role.getLabel());
    }
}
