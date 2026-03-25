package com.kfd.healthmenu.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSummaryDto {
    private Long id;
    private String username;
    private String displayName;
    private String roleCode;
    private String roleLabel;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
