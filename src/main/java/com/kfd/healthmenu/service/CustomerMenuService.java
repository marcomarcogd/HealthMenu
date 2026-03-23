package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.menu.CustomerMenuSummaryDto;
import com.kfd.healthmenu.entity.CustomerMenu;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface CustomerMenuService {
    List<CustomerMenu> listAll();

    List<CustomerMenuSummaryDto> listSummaries();

    CustomerMenu getById(Long id);

    CustomerMenuForm buildCreateForm(Long customerId, Long templateId);

    CustomerMenuForm buildCreateFormFromAi(Long customerId, Long templateId, AiImportResultDto aiImportResultDto);

    AiImportResultDto parseAiMenuText(String sourceText);

    String generateAiImage(String prompt);

    CustomerMenuForm getFormById(Long id);

    Long saveMenu(CustomerMenuForm form);

    void publishMenu(Long id);

    void exportMenuExcel(Long id, HttpServletResponse response);

    void deleteById(Long id);
}
