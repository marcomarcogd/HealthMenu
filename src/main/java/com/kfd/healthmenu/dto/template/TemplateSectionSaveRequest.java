package com.kfd.healthmenu.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateSectionSaveRequest {
    private Long id;
    @NotBlank
    private String sectionType;
    @NotBlank
    private String title;
    private Boolean enabled;
    private Boolean allowImage;
    private String styleConfigJson;
}
