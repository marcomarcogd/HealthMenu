package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_template_meal_item")
public class MenuTemplateMealItem extends BaseEntity {

    private Long templateMealId;
    private String itemCode;
    private String itemName;
    private String contentFormat;
    private Integer sortOrder;
    private Integer enabled;
    private Integer allowImage;
}
