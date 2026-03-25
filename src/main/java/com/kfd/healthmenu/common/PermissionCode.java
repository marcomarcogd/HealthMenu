package com.kfd.healthmenu.common;

public enum PermissionCode {
    DASHBOARD_VIEW("工作台查看", "工作台", "查看后台首页概览、统计卡片和运营提醒"),
    OPTIONS_READ("基础选项读取", "基础数据", "读取客户、模板等下拉选项和初始化基础数据"),
    CUSTOMER_MANAGE("客户管理", "客户管理", "创建、编辑、删除客户资料"),
    MENU_MANAGE("餐单管理", "餐单管理", "创建、编辑、发布、导出餐单"),
    TEMPLATE_MANAGE("模板管理", "模板设计", "设计和维护餐单模板结构"),
    DICT_MANAGE("字典管理", "基础数据", "维护字典项和可选配置"),
    USER_MANAGE("账号管理", "账号安全", "创建账号、调整状态、重置密码"),
    ROLE_MANAGE("角色权限管理", "账号安全", "创建角色并配置角色权限");

    private final String label;
    private final String groupLabel;
    private final String description;

    PermissionCode(String label, String groupLabel, String description) {
        this.label = label;
        this.groupLabel = groupLabel;
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public String getDescription() {
        return description;
    }
}
