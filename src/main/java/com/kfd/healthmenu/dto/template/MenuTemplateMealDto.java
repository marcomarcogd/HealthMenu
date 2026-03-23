package com.kfd.healthmenu.dto.template;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTemplateMealDto {
    private Long id;
    private String mealCode;
    private String mealName;
    private String timeLabel;
    private Integer sortOrder;
    private Boolean enabled;
    private List<MenuTemplateMealItemDto> items = new ArrayList<>();
}
