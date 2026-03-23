package com.kfd.healthmenu.dto;

import com.kfd.healthmenu.entity.MenuTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TemplateOptionDto {
    private Long id;
    private String name;

    public static TemplateOptionDto from(MenuTemplate template) {
        return new TemplateOptionDto(template.getId(), template.getName());
    }
}
