package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class CustomerMenuMealItemForm {
    private String itemCode;
    private String itemName;
    private String itemValue;
    private String color;
    private Boolean bold;
    private Boolean allowImage;
    private String imagePath;
    private String aiImagePrompt;
    private Integer sortOrder;
}
