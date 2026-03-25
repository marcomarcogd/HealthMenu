package com.kfd.healthmenu.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CurrentUserDto {
    private Long id;
    private String username;
    private String displayName;
    private String roleCode;
    private String roleLabel;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
