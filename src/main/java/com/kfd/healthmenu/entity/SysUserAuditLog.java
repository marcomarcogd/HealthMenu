package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_user_audit_log")
@EqualsAndHashCode(callSuper = true)
public class SysUserAuditLog extends BaseEntity {
    private Long targetUserId;
    private String targetUsername;
    private String targetDisplayName;
    private Long operatorUserId;
    private String operatorUsername;
    private String operatorDisplayName;
    private String actionCode;
    private String actionLabel;
    private String detail;
}
