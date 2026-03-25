package com.kfd.healthmenu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerMenuForm {

    private Long id;

    @NotNull(message = "请选择客户")
    private Long customerId;

    @NotNull(message = "请选择模板")
    private Long templateId;

    @NotNull(message = "请选择餐单日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate menuDate;

    private Integer weekIndex;
    private String title;
    private String themeCode;
    private String themeName;
    private Boolean showWeeklyTip;
    private Boolean showSwapGuide;
    private String status;
    private String statusLabel;
    private String viewUrl;
    private String shareUrl;
    private Integer publishCount;
    private LocalDateTime lastPublishedAt;
    private List<CustomerMenuSectionForm> sections = new ArrayList<>();
    private List<CustomerMenuMealForm> meals = new ArrayList<>();
}
