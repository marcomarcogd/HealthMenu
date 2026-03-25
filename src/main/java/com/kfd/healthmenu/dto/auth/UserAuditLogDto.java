package com.kfd.healthmenu.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAuditLogDto {
    private Long id;
    private Long targetUserId;
    private String targetUsername;
    private String targetDisplayName;
    private Long operatorUserId;
    private String operatorUsername;
    private String operatorDisplayName;
    private String actionCode;
    private String actionLabel;
    private String detail;
    private LocalDateTime createTime;
}
