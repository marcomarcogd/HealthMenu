package com.kfd.healthmenu.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleDto {
    private Long id;
    private String roleCode;
    private String roleName;
    private List<String> permissionCodes;
    private List<String> permissionLabels;
    private Integer isSystem;
    private Long userCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
