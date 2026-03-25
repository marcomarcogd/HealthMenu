package com.kfd.healthmenu.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateMealSaveRequest {
    private Long id;
    @NotBlank(message = "餐次编码不能为空")
    private String mealCode;
    @NotBlank(message = "餐次名称不能为空")
    private String mealName;
    private String timeLabel;
    private Boolean enabled;
    @Valid
    private List<TemplateMealItemSaveRequest> items = new ArrayList<>();
}
