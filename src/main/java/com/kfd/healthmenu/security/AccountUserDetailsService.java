package com.kfd.healthmenu.security;

import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountService accountService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = accountService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getDisplayName(),
                user.getRoleCode(),
                user.getStatus()
        );
    }
}
