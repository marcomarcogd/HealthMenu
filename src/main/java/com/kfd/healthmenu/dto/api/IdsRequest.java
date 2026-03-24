package com.kfd.healthmenu.dto.api;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IdsRequest {

    @NotEmpty(message = "请至少选择一条餐单")
    private List<Long> ids;
}
