package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.AiImportResultDto;

public interface AiImportService {
    AiImportResultDto parseMenuText(String sourceText);

    String generateImage(String prompt);
}
