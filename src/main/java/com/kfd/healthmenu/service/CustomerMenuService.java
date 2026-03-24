package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.api.PageResult;
import com.kfd.healthmenu.dto.menu.CustomerMenuSummaryDto;
import com.kfd.healthmenu.dto.menu.MenuSummaryQuery;
import com.kfd.healthmenu.entity.CustomerMenu;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface CustomerMenuService {
    List<CustomerMenu> listAll();

    PageResult<CustomerMenuSummaryDto> listSummaries(MenuSummaryQuery query);

    CustomerMenu getById(Long id);

    CustomerMenuForm buildCreateForm(Long customerId, Long templateId);

    CustomerMenuForm buildCreateFormFromAi(Long customerId, Long templateId, AiImportResultDto aiImportResultDto);

    AiImportResultDto parseAiMenuText(String sourceText);

    String generateAiImage(String prompt);

    CustomerMenuForm getFormById(Long id);

    Long saveMenu(CustomerMenuForm form);

    void publishMenu(Long id);

    void publishMenus(List<Long> ids);

    void exportMenuExcel(Long id, HttpServletResponse response);

    void exportMenusExcel(List<Long> ids, HttpServletResponse response);

    void deleteById(Long id);
}
