package com.kfd.healthmenu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiImportResultDto {
    private String title;
    private Integer weekIndex;
    private String weeklyTip;
    private String swapGuide;
    private String parseMode;
    private String parseMessage;
    private List<CustomerMenuMealForm> meals = new ArrayList<>();
}
