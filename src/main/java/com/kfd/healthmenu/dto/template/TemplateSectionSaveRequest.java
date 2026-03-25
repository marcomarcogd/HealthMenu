package com.kfd.healthmenu.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateSectionSaveRequest {
    private Long id;
    @NotBlank(message = "区块类型不能为空")
    private String sectionType;
    @NotBlank(message = "区块标题不能为空")
    private String title;
    private Boolean enabled;
    private Boolean allowImage;
    private String styleConfigJson;
}
