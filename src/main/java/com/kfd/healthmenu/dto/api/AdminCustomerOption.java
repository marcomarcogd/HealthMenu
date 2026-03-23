package com.kfd.healthmenu.dto.api;

import lombok.Data;

@Data
public class AdminCustomerOption {
    private String label;
    private String value;
    private String nickname;
    private String gender;
    private String phone;
    private String exclusiveTitle;
    private String note;
    private Integer status;
}
