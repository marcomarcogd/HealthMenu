package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_menu_section_content")
public class CustomerMenuSectionContent extends BaseEntity {

    private Long customerMenuId;
    private String sectionType;
    private String title;
    private String content;
    private String styleJson;
    private String imagePath;
    private String aiImagePrompt;
    private String aiImageTaskId;
    private Integer sortOrder;
}
