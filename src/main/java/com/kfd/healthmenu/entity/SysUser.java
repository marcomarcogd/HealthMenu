package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String displayName;
    private String roleCode;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
