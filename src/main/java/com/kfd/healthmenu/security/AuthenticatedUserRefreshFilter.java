package com.kfd.healthmenu.security;

import com.kfd.healthmenu.entity.SysUser;
import com.kfd.healthmenu.service.AccountService;
import com.kfd.healthmenu.service.RoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserRefreshFilter extends OncePerRequestFilter {

    private final AccountService accountService;
    private final RoleService roleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof AuthenticatedUser currentUser) {
            SysUser latestUser = accountService.findByUsername(currentUser.getUsername());
            if (latestUser == null) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                }
            } else {
                try {
                    AuthenticatedUser refreshedUser = new AuthenticatedUser(
                            latestUser.getId(),
                            latestUser.getUsername(),
                            latestUser.getPassword(),
                            latestUser.getDisplayName(),
                            latestUser.getRoleCode(),
                            roleService.resolveRoleName(latestUser.getRoleCode()),
                            latestUser.getStatus(),
                            roleService.resolvePermissionCodes(latestUser.getRoleCode())
                    );
                    UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
                            refreshedUser,
                            authentication.getCredentials(),
                            refreshedUser.getAuthorities()
                    );
                    refreshed.setDetails(authentication.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(refreshed);
                } catch (Exception ex) {
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) {
                        request.getSession(false).removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
