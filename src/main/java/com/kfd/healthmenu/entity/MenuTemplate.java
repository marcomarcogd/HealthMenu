package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_template")
public class MenuTemplate extends BaseEntity {

    private String name;
    private String description;
    private Integer isDefault;
    private String themeCode;
    private String coverImagePath;
    private String titleRule;
    private Integer status;
}
