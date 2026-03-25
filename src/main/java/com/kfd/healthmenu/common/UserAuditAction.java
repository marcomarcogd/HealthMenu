package com.kfd.healthmenu.common;

public enum UserAuditAction {
    CREATE("创建账号"),
    UPDATE("编辑账号"),
    RESET_PASSWORD("重置密码"),
    DELETE("删除账号");

    private final String label;

    UserAuditAction(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }
}
