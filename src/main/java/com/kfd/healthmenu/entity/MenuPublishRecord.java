package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_publish_record")
public class MenuPublishRecord extends BaseEntity {

    private Long customerMenuId;
    private String exportType;
    private String filePath;
    private String fileName;
    private String operatorName;
}
