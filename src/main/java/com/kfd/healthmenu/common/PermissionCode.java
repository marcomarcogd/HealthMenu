package com.kfd.healthmenu.common;

public enum PermissionCode {
    DASHBOARD_VIEW("工作台查看"),
    OPTIONS_READ("基础选项读取"),
    CUSTOMER_MANAGE("客户管理"),
    MENU_MANAGE("餐单管理"),
    TEMPLATE_MANAGE("模板管理"),
    DICT_MANAGE("字典管理"),
    USER_MANAGE("账号管理"),
    ROLE_MANAGE("角色权限管理");

    private final String label;

    PermissionCode(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }
}
