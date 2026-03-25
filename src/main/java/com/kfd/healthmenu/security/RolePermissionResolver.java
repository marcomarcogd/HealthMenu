package com.kfd.healthmenu.security;

import com.kfd.healthmenu.common.PermissionCode;
import com.kfd.healthmenu.common.UserRole;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RolePermissionResolver {

    private static final Map<UserRole, Set<PermissionCode>> ROLE_PERMISSIONS = Map.of(
            UserRole.ADMIN, EnumSet.allOf(PermissionCode.class),
            UserRole.HEALTH_MANAGER, EnumSet.of(
                    PermissionCode.DASHBOARD_VIEW,
                    PermissionCode.OPTIONS_READ,
                    PermissionCode.CUSTOMER_MANAGE,
                    PermissionCode.MENU_MANAGE
            )
    );

    private RolePermissionResolver() {
    }

    public static Set<PermissionCode> resolve(UserRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(PermissionCode.class));
    }

    public static List<String> resolveCodes(String roleCode) {
        return resolve(UserRole.fromCode(roleCode)).stream()
                .map(PermissionCode::getCode)
                .sorted()
                .toList();
    }
}
