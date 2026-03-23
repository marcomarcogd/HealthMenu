package com.kfd.healthmenu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTemplateMealDesignForm {
    private Long id;
    private String mealCode;
    private String mealName;
    private String timeLabel;
    private Integer sortOrder;
    private Boolean enabled;
    private List<MenuTemplateMealItemDesignForm> items = new ArrayList<>();
}
