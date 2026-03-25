package com.kfd.healthmenu.security;

import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.service.AccountService;
import com.kfd.healthmenu.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountService accountService;
    private final RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = accountService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }
        try {
            roleService.requireByRoleCode(user.getRoleCode());
        } catch (Exception ex) {
            throw new UsernameNotFoundException("账号角色不存在或已失效");
        }
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getDisplayName(),
                user.getRoleCode(),
                roleService.resolveRoleName(user.getRoleCode()),
                user.getStatus(),
                roleService.resolvePermissionCodes(user.getRoleCode())
        );
    }
}
