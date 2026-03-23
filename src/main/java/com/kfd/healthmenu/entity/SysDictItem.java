package com.kfd.healthmenu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private Long dictTypeId;
    private String itemCode;
    private String itemLabel;
    private String itemValue;
    private Integer sortOrder;
    private Integer isSystem;
    private Integer status;
}
