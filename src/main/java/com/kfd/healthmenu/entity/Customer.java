package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {

    private String name;
    private String nickname;
    private String gender;
    private String phone;
    private String exclusiveTitle;
    private String note;
    private Integer status;
}
