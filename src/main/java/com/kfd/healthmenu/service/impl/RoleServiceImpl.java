package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.common.PermissionCode;
import com.kfd.healthmenu.common.UserRole;
import com.kfd.healthmenu.dto.auth.PermissionOptionDto;
import com.kfd.healthmenu.dto.auth.RoleDto;
import com.kfd.healthmenu.dto.auth.RoleSaveRequest;
import com.kfd.healthmenu.entity.SysRole;
import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.mapper.SysRoleMapper;
import com.kfd.healthmenu.mapper.SysUserMapper;
import com.kfd.healthmenu.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final Map<String, String> PERMISSION_LABELS = Arrays.stream(PermissionCode.values())
            .collect(java.util.stream.Collectors.toMap(PermissionCode::getCode, PermissionCode::getLabel));

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<RoleDto> listRoles() {
        Map<String, Long> roleUserCountMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(SysUser::getRoleCode, java.util.stream.Collectors.counting()));

        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getDeleted, 0)
                        .orderByDesc(SysRole::getUpdateTime)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(role -> toDto(role, roleUserCountMap.getOrDefault(role.getRoleCode(), 0L)))
                .toList();
    }

    @Override
    public List<PermissionOptionDto> listPermissionOptions() {
        return Arrays.stream(PermissionCode.values())
                .map(item -> new PermissionOptionDto(
                        item.getCode(),
                        item.getLabel(),
                        item.getGroupLabel(),
                        item.getDescription()
                ))
                .toList();
    }

    @Override
    @Transactional
    public RoleDto saveRole(RoleSaveRequest request) {
        String roleCode = normalizeRoleCode(request.getRoleCode());
        String roleName = normalizeRoleName(request.getRoleName());
        List<String> permissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        ensureRequiredSystemPermissions(roleCode, permissionCodes);

        if (request.getId() == null) {
            if (findByRoleCode(roleCode) != null) {
                throw new BizException("ROLE_CODE_EXISTS", "角色编码已存在，请更换后再试");
            }
            SysRole role = new SysRole();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setPermissionCodesJson(writePermissionCodes(permissionCodes));
            role.setIsSystem(0);
            sysRoleMapper.insert(role);
            return toDto(sysRoleMapper.selectById(role.getId()), 0L);
        }

        SysRole existing = requireById(request.getId());
        if (!existing.getRoleCode().equals(roleCode)) {
            throw new BizException("ROLE_CODE_IMMUTABLE", "编辑角色时不支持修改角色编码");
        }
        existing.setRoleName(roleName);
        existing.setPermissionCodesJson(writePermissionCodes(permissionCodes));
        sysRoleMapper.updateById(existing);
        Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(SysUser::getRoleCode, existing.getRoleCode()));
        return toDto(sysRoleMapper.selectById(existing.getId()), userCount == null ? 0L : userCount);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        SysRole role = requireById(roleId);
        if (Integer.valueOf(1).equals(role.getIsSystem())) {
            throw new BizException("SYSTEM_ROLE_DELETE_FORBIDDEN", "系统内置角色不支持删除");
        }
        Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(SysUser::getRoleCode, role.getRoleCode()));
        if (userCount != null && userCount > 0) {
            throw new BizException("ROLE_IN_USE", "该角色仍有账号在使用，不能删除");
        }
        sysRoleMapper.deleteById(roleId);
    }

    @Override
    @Transactional
    public void ensureSystemRole(String roleCode, String roleName, List<String> permissionCodes) {
        SysRole existing = findByRoleCode(roleCode);
        if (existing != null) {
            return;
        }
        SysRole role = new SysRole();
        role.setRoleCode(normalizeRoleCode(roleCode));
        role.setRoleName(normalizeRoleName(roleName));
        role.setPermissionCodesJson(writePermissionCodes(normalizePermissionCodes(permissionCodes)));
        role.setIsSystem(1);
        sysRoleMapper.insert(role);
    }

    @Override
    public SysRole findByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }
        return sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode.trim().toUpperCase(Locale.ROOT))
                .eq(SysRole::getDeleted, 0)
                .last("limit 1"));
    }

    @Override
    public SysRole requireByRoleCode(String roleCode) {
        SysRole role = findByRoleCode(roleCode);
        if (role == null) {
            throw new BizException("ROLE_NOT_FOUND", "角色不存在，请先到角色管理中检查配置");
        }
        return role;
    }

    @Override
    public String resolveRoleName(String roleCode) {
        SysRole role = findByRoleCode(roleCode);
        return role == null ? roleCode : role.getRoleName();
    }

    @Override
    public List<String> resolvePermissionCodes(String roleCode) {
        SysRole role = requireByRoleCode(roleCode);
        return readPermissionCodes(role.getPermissionCodesJson());
    }

    @Override
    public List<String> resolvePermissionLabels(List<String> permissionCodes) {
        return normalizePermissionCodes(permissionCodes).stream()
                .map(code -> PERMISSION_LABELS.getOrDefault(code, code))
                .toList();
    }

    private SysRole requireById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || Integer.valueOf(1).equals(role.getDeleted())) {
            throw new BizException("ROLE_NOT_FOUND", "未找到对应角色");
        }
        return role;
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException("INVALID_ROLE_CODE", "角色编码不能为空");
        }
        if (!ROLE_CODE_PATTERN.matcher(normalized).matches()) {
            throw new BizException("INVALID_ROLE_CODE", "角色编码仅支持大写字母、数字和下划线，且需以字母开头");
        }
        return normalized;
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException("INVALID_ROLE_NAME", "角色名称不能为空");
        }
        return normalized;
    }

    private List<String> normalizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new BizException("INVALID_PERMISSIONS", "请至少选择一个权限");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            String code = permissionCode == null ? "" : permissionCode.trim();
            if (!PERMISSION_LABELS.containsKey(code)) {
                throw new BizException("INVALID_PERMISSIONS", "权限配置中包含不支持的权限项");
            }
            normalized.add(code);
        }
        return normalized.stream().sorted().toList();
    }

    private String writePermissionCodes(List<String> permissionCodes) {
        try {
            return objectMapper.writeValueAsString(permissionCodes);
        } catch (JsonProcessingException ex) {
            throw new BizException("ROLE_PERMISSION_SERIALIZE_FAILED", "角色权限保存失败，请稍后重试");
        }
    }

    private List<String> readPermissionCodes(String permissionCodesJson) {
        try {
            List<String> parsed = objectMapper.readValue(
                    StringUtils.hasText(permissionCodesJson) ? permissionCodesJson : "[]",
                    STRING_LIST_TYPE
            );
            return normalizePermissionCodes(parsed.isEmpty() ? List.of(PermissionCode.DASHBOARD_VIEW.getCode()) : parsed);
        } catch (JsonProcessingException ex) {
            throw new BizException("ROLE_PERMISSION_PARSE_FAILED", "角色权限配置损坏，请联系管理员修复");
        }
    }

    private void ensureRequiredSystemPermissions(String roleCode, List<String> permissionCodes) {
        if (!UserRole.ADMIN.getCode().equals(roleCode)) {
            return;
        }
        if (!permissionCodes.contains(PermissionCode.USER_MANAGE.getCode())
                || !permissionCodes.contains(PermissionCode.ROLE_MANAGE.getCode())) {
            throw new BizException("ADMIN_ROLE_PERMISSION_REQUIRED", "管理员角色至少要保留账号管理和角色权限管理");
        }
    }

    private RoleDto toDto(SysRole role, Long userCount) {
        List<String> permissionCodes = readPermissionCodes(role.getPermissionCodesJson());
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setPermissionCodes(permissionCodes);
        dto.setPermissionLabels(resolvePermissionLabels(permissionCodes));
        dto.setIsSystem(role.getIsSystem());
        dto.setUserCount(userCount == null ? 0L : userCount);
        dto.setCreateTime(role.getCreateTime());
        dto.setUpdateTime(role.getUpdateTime());
        return dto;
    }
}
