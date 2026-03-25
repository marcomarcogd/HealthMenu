package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.auth.PermissionOptionDto;
import com.kfd.healthmenu.dto.auth.RoleDto;
import com.kfd.healthmenu.dto.auth.RoleSaveRequest;
import com.kfd.healthmenu.entity.SysRole;

import java.util.List;

public interface RoleService {
    List<RoleDto> listRoles();

    List<PermissionOptionDto> listPermissionOptions();

    RoleDto saveRole(RoleSaveRequest request);

    void deleteRole(Long roleId);

    void ensureSystemRole(String roleCode, String roleName, List<String> permissionCodes);

    SysRole findByRoleCode(String roleCode);

    SysRole requireByRoleCode(String roleCode);

    String resolveRoleName(String roleCode);

    List<String> resolvePermissionCodes(String roleCode);

    List<String> resolvePermissionLabels(List<String> permissionCodes);
}
