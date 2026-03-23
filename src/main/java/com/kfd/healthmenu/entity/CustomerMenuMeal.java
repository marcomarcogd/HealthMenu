package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_menu_meal")
public class CustomerMenuMeal extends BaseEntity {

    private Long customerMenuId;
    private String mealCode;
    private String mealName;
    private String timeLabel;
    private String mealTime;
    private Integer sortOrder;
}
