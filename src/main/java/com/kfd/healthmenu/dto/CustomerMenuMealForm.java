package com.kfd.healthmenu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerMenuMealForm {
    private String mealCode;
    private String mealName;
    private String timeLabel;
    private String mealTime;
    private Integer sortOrder;
    private List<CustomerMenuMealItemForm> items = new ArrayList<>();
}
