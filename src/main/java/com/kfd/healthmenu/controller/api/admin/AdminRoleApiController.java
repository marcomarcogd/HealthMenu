package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.auth.PermissionOptionDto;
import com.kfd.healthmenu.dto.auth.RoleDto;
import com.kfd.healthmenu.dto.auth.RoleSaveRequest;
import com.kfd.healthmenu.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleApiController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_MANAGE')")
    public ApiResponse<List<RoleDto>> list() {
        return ApiResponse.success(roleService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_MANAGE')")
    public ApiResponse<List<PermissionOptionDto>> listPermissions() {
        return ApiResponse.success(roleService.listPermissionOptions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ApiResponse<RoleDto> save(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success("角色保存成功", roleService.saveRole(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success("角色已删除", null);
    }
}
