package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.auth.CurrentUserDto;
import com.kfd.healthmenu.dto.auth.UserSaveRequest;
import com.kfd.healthmenu.dto.auth.UserSummaryDto;
import com.kfd.healthmenu.entity.SysUser;

import java.util.List;

public interface AccountService {
    List<UserSummaryDto> listUsers();

    UserSummaryDto saveUser(UserSaveRequest request);

    void resetPassword(Long userId, String password);

    void deleteUser(Long userId, Long operatorUserId);

    CurrentUserDto getCurrentUser(Long userId);

    SysUser findByUsername(String username);

    void recordLoginSuccess(Long userId);
}
