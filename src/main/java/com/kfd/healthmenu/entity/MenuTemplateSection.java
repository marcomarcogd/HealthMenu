package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_template_section")
public class MenuTemplateSection extends BaseEntity {

    private Long templateId;
    private String sectionType;
    private String title;
    private Integer sortOrder;
    private Integer enabled;
    private String styleConfigJson;
    private Integer allowImage;
}
