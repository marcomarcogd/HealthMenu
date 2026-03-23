package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_menu")
public class CustomerMenu extends BaseEntity {

    private Long customerId;
    private Long templateId;
    private LocalDate menuDate;
    private Integer weekIndex;
    private String title;
    private Integer showWeeklyTip;
    private Integer showSwapGuide;
    private String status;
    private String shareToken;
    private String themeCode;
}
