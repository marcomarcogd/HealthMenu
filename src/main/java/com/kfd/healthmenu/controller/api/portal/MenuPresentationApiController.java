package com.kfd.healthmenu.controller.api.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.RecordStatus;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.menu.MenuPresentationDto;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.mapper.CustomerMenuMapper;
import com.kfd.healthmenu.service.CustomerMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/menus")
@RequiredArgsConstructor
public class MenuPresentationApiController {

    private final CustomerMenuService customerMenuService;
    private final CustomerMenuMapper customerMenuMapper;

    @GetMapping("/{id}")
    public ApiResponse<MenuPresentationDto> view(@PathVariable Long id) {
        CustomerMenu menu = customerMenuMapper.selectOne(new LambdaQueryWrapper<CustomerMenu>()
                .eq(CustomerMenu::getId, id)
                .eq(CustomerMenu::getDeleted, 0)
                .last("limit 1"));
        return ApiResponse.success(buildPayload(menu, false));
    }

    @GetMapping("/share/{token}")
    public ApiResponse<MenuPresentationDto> share(@PathVariable String token) {
        CustomerMenu menu = customerMenuMapper.selectOne(new LambdaQueryWrapper<CustomerMenu>()
                .eq(CustomerMenu::getShareToken, token)
                .eq(CustomerMenu::getStatus, RecordStatus.PUBLISHED.name())
                .eq(CustomerMenu::getDeleted, 0)
                .last("limit 1"));
        if (menu == null) {
            throw new BizException("MENU_NOT_PUBLISHED", "餐单尚未发布，暂不能通过分享链接查看");
        }
        return ApiResponse.success(buildPayload(menu, true));
    }

    private MenuPresentationDto buildPayload(CustomerMenu menu, boolean shareMode) {
        if (menu == null) {
            throw new BizException("MENU_NOT_FOUND", "未找到对应的餐单");
        }
        CustomerMenuForm form = customerMenuService.getFormById(menu.getId());
        if (form == null) {
            throw new BizException("MENU_NOT_FOUND", "未找到对应的餐单");
        }
        MenuPresentationDto dto = new MenuPresentationDto();
        dto.setMenuForm(form);
        dto.setShareMode(shareMode);
        return dto;
    }
}
