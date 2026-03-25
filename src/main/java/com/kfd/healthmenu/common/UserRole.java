package com.kfd.healthmenu.common;

import java.util.Arrays;

public enum UserRole {
    ADMIN("管理员"),
    HEALTH_MANAGER("健管师");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static UserRole fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的角色：" + code));
    }
}
