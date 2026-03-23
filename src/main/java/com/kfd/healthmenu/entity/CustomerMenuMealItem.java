package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_menu_meal_item")
public class CustomerMenuMealItem extends BaseEntity {

    private Long customerMenuMealId;
    private String itemCode;
    private String itemName;
    private String itemValue;
    private String styleJson;
    private String imagePath;
    private String aiImagePrompt;
    private String aiImageTaskId;
    private Integer sortOrder;
}
