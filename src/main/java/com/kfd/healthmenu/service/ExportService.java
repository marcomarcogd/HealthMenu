package com.kfd.healthmenu.service;

import com.kfd.healthmenu.entity.CustomerMenu;
import jakarta.servlet.http.HttpServletResponse;

public interface ExportService {
    String exportMenuExcel(CustomerMenu menu, HttpServletResponse response);
}
