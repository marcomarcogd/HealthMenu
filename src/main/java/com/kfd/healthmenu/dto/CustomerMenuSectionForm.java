package com.kfd.healthmenu.dto;

import lombok.Data;

@Data
public class CustomerMenuSectionForm {
    private String sectionType;
    private String title;
    private String content;
    private String color;
    private Boolean bold;
    private Boolean allowImage;
    private String imagePath;
    private String aiImagePrompt;
    private Integer sortOrder;
}
