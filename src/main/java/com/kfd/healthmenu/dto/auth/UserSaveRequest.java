package com.kfd.healthmenu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSaveRequest {
    private Long id;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String displayName;

    @NotBlank(message = "角色不能为空")
    private String roleCode;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String password;
}
