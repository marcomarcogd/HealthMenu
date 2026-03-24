package com.kfd.healthmenu.dto.menu;

import lombok.Data;

@Data
public class MenuSummaryQuery {
    private String keyword;
    private Long customerId;
    private String status;
    private String sort = "menuDateDesc";
    private long page = 1L;
    private long pageSize = 10L;
}
