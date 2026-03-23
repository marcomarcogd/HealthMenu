package com.kfd.healthmenu.config;

import com.kfd.healthmenu.dto.AiImportResultDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class SessionStateConfig {

    @Bean
    @SessionScope
    public AiImportResultDto aiImportSessionResult() {
        return new AiImportResultDto();
    }
}
