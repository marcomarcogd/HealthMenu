package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.auth.UserAuditLogDto;
import com.kfd.healthmenu.dto.auth.UserPasswordResetRequest;
import com.kfd.healthmenu.dto.auth.UserSaveRequest;
import com.kfd.healthmenu.dto.auth.UserSummaryDto;
import com.kfd.healthmenu.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class AdminUserApiController {

    private final AccountService accountService;

    @GetMapping
    public ApiResponse<List<UserSummaryDto>> list() {
        return ApiResponse.success(accountService.listUsers());
    }

    @GetMapping("/audits")
    public ApiResponse<List<UserAuditLogDto>> listAudits(@org.springframework.web.bind.annotation.RequestParam(required = false) Long targetUserId) {
        return ApiResponse.success(accountService.listAuditLogs(targetUserId));
    }

    @PostMapping
    public ApiResponse<UserSummaryDto> save(@Valid @RequestBody UserSaveRequest request,
                                            Authentication authentication) {
        return ApiResponse.success("账号保存成功", accountService.saveUser(request, resolveOperatorUserId(authentication)));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody UserPasswordResetRequest request,
                                           Authentication authentication) {
        accountService.resetPassword(id, request.getPassword(), resolveOperatorUserId(authentication));
        return ApiResponse.success("密码已重置", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    Authentication authentication) {
        accountService.deleteUser(id, resolveOperatorUserId(authentication));
        return ApiResponse.success("账号已删除", null);
    }

    private Long resolveOperatorUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        var operator = accountService.findByUsername(authentication.getName());
        return operator == null ? null : operator.getId();
    }
}
