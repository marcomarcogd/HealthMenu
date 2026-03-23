package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_import_record")
public class AiImportRecord extends BaseEntity {

    private Long customerId;
    private String sourceType;
    private String sourceText;
    private String sourceImagePath;
    private String parsedJson;
    private String status;
    private String provider;
    private String workflowCode;
}
