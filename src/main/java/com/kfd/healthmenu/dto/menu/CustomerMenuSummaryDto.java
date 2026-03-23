package com.kfd.healthmenu.dto.menu;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerMenuSummaryDto {
    private Long id;
    private Long customerId;
    private Long templateId;
    private LocalDate menuDate;
    private Integer weekIndex;
    private String title;
    private String status;
    private String statusLabel;
    private String themeCode;
    private String themeName;
    private String viewUrl;
    private String shareUrl;
    private Integer publishCount;
    private LocalDateTime lastPublishedAt;
}
