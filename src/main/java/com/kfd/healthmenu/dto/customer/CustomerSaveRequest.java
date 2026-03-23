package com.kfd.healthmenu.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerSaveRequest {
    private Long id;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String nickname;
    private String gender;
    private String phone;
    private String exclusiveTitle;
    private String note;
    private Integer status;
}
