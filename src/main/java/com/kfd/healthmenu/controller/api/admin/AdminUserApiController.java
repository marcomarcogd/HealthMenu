package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.auth.UserPasswordResetRequest;
import com.kfd.healthmenu.dto.auth.UserSaveRequest;
import com.kfd.healthmenu.dto.auth.UserSummaryDto;
import com.kfd.healthmenu.security.AuthenticatedUser;
import com.kfd.healthmenu.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserApiController {

    private final AccountService accountService;

    @GetMapping
    public ApiResponse<List<UserSummaryDto>> list() {
        return ApiResponse.success(accountService.listUsers());
    }

    @PostMapping
    public ApiResponse<UserSummaryDto> save(@Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.success("账号保存成功", accountService.saveUser(request));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody UserPasswordResetRequest request) {
        accountService.resetPassword(id, request.getPassword());
        return ApiResponse.success("密码已重置", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        accountService.deleteUser(id, currentUser == null ? null : currentUser.getId());
        return ApiResponse.success("账号已删除", null);
    }
}
