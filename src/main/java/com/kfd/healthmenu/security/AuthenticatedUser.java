package com.kfd.healthmenu.security;

import com.kfd.healthmenu.common.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String displayName;
    private final String roleCode;
    private final Integer status;
    private final List<String> permissions;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, String username, String password, String displayName, String roleCode, Integer status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.status = status;
        this.permissions = RolePermissionResolver.resolveCodes(roleCode);
        Set<GrantedAuthority> resolvedAuthorities = new LinkedHashSet<>();
        resolvedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + UserRole.fromCode(roleCode).getCode()));
        permissions.forEach(code -> resolvedAuthorities.add(new SimpleGrantedAuthority(code)));
        this.authorities = List.copyOf(resolvedAuthorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
