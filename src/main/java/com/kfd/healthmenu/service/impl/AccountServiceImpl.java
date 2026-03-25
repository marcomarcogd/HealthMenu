package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.common.UserRole;
import com.kfd.healthmenu.dto.auth.CurrentUserDto;
import com.kfd.healthmenu.dto.auth.UserSaveRequest;
import com.kfd.healthmenu.dto.auth.UserSummaryDto;
import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.mapper.SysUserMapper;
import com.kfd.healthmenu.security.RolePermissionResolver;
import com.kfd.healthmenu.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserSummaryDto> listUsers() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0)
                        .orderByDesc(SysUser::getUpdateTime)
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public UserSummaryDto saveUser(UserSaveRequest request) {
        String username = normalizeUsername(request.getUsername());
        String displayName = normalizeText(request.getDisplayName());
        String roleCode = normalizeRoleCode(request.getRoleCode());
        Integer status = normalizeStatus(request.getStatus());

        SysUser duplicate = findByUsername(username);
        if (duplicate != null && !duplicate.getId().equals(request.getId())) {
            throw new BizException("USERNAME_EXISTS", "账号已存在，请换一个账号名");
        }

        if (request.getId() == null) {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setRoleCode(roleCode);
            user.setStatus(status);
            user.setPassword(passwordEncoder.encode(requireValidPassword(request.getPassword(), true)));
            sysUserMapper.insert(user);
            return toSummary(sysUserMapper.selectById(user.getId()));
        }

        SysUser existing = requireUser(request.getId());
        ensureLastAdminWillRemain(existing, roleCode, status);

        existing.setUsername(username);
        existing.setDisplayName(displayName);
        existing.setRoleCode(roleCode);
        existing.setStatus(status);
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(passwordEncoder.encode(requireValidPassword(request.getPassword(), false)));
        }
        sysUserMapper.updateById(existing);
        return toSummary(sysUserMapper.selectById(existing.getId()));
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String password) {
        SysUser user = requireUser(userId);
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(requireValidPassword(password, false)));
        sysUserMapper.updateById(update);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Long operatorUserId) {
        if (userId == null) {
            throw new BizException("USER_NOT_FOUND", "未找到要删除的账号");
        }
        if (userId.equals(operatorUserId)) {
            throw new BizException("DELETE_SELF_FORBIDDEN", "不能删除当前登录账号");
        }
        SysUser user = requireUser(userId);
        ensureLastAdminWillRemain(user, user.getRoleCode(), 0);
        sysUserMapper.deleteById(userId);
    }

    @Override
    public CurrentUserDto getCurrentUser(Long userId) {
        return toCurrentUser(requireUser(userId));
    }

    @Override
    public SysUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .eq(SysUser::getDeleted, 0)
                .last("limit 1"));
    }

    @Override
    public void recordLoginSuccess(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BizException("USER_NOT_FOUND", "未找到对应账号");
        }
        return user;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BizException("VALIDATION_ERROR", "账号不能为空");
        }
        return username.trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("INVALID_STATUS", "账号状态不正确");
        }
        return status;
    }

    private String normalizeRoleCode(String roleCode) {
        try {
            return UserRole.fromCode(roleCode == null ? "" : roleCode.trim()).getCode();
        } catch (IllegalArgumentException ex) {
            throw new BizException("INVALID_ROLE", "账号角色不正确");
        }
    }

    private String requireValidPassword(String password, boolean createMode) {
        String normalized = password == null ? "" : password.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException("INVALID_PASSWORD", createMode ? "创建账号时必须设置密码" : "新密码不能为空");
        }
        if (normalized.length() < 8) {
            throw new BizException("INVALID_PASSWORD", "密码至少需要 8 位");
        }
        return normalized;
    }

    private void ensureLastAdminWillRemain(SysUser existing, String nextRoleCode, Integer nextStatus) {
        boolean wasActiveAdmin = UserRole.ADMIN.getCode().equals(existing.getRoleCode()) && Integer.valueOf(1).equals(existing.getStatus());
        boolean keepsActiveAdmin = UserRole.ADMIN.getCode().equals(nextRoleCode) && Integer.valueOf(1).equals(nextStatus);
        if (!wasActiveAdmin || keepsActiveAdmin) {
            return;
        }

        Long remaining = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(SysUser::getRoleCode, UserRole.ADMIN.getCode())
                .eq(SysUser::getStatus, 1)
                .ne(SysUser::getId, existing.getId()));
        if (remaining == null || remaining == 0L) {
            throw new BizException("LAST_ADMIN_REQUIRED", "系统至少需要保留一个启用中的管理员账号");
        }
    }

    private CurrentUserDto toCurrentUser(SysUser user) {
        CurrentUserDto dto = new CurrentUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setRoleCode(user.getRoleCode());
        dto.setRoleLabel(resolveRoleLabel(user.getRoleCode()));
        dto.setStatus(user.getStatus());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setPermissions(RolePermissionResolver.resolveCodes(user.getRoleCode()));
        return dto;
    }

    private UserSummaryDto toSummary(SysUser user) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setRoleCode(user.getRoleCode());
        dto.setRoleLabel(resolveRoleLabel(user.getRoleCode()));
        dto.setStatus(user.getStatus());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    private String resolveRoleLabel(String roleCode) {
        try {
            return UserRole.fromCode(roleCode).getLabel();
        } catch (IllegalArgumentException ex) {
            return roleCode;
        }
    }
}
