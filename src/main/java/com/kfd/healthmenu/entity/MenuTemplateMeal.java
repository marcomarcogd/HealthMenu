package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_template_meal")
public class MenuTemplateMeal extends BaseEntity {

    private Long templateId;
    private String mealCode;
    private String mealName;
    private String timeLabel;
    private Integer sortOrder;
    private Integer enabled;
}
