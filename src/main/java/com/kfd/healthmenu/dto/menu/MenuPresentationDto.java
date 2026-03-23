package com.kfd.healthmenu.dto.menu;

import com.kfd.healthmenu.dto.CustomerMenuForm;
import lombok.Data;

@Data
public class MenuPresentationDto {
    private CustomerMenuForm menuForm;
    private boolean shareMode;
}
