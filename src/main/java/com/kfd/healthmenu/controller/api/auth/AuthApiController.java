package com.kfd.healthmenu.controller.api.auth;

import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.auth.CurrentUserDto;
import com.kfd.healthmenu.dto.auth.LoginRequest;
import com.kfd.healthmenu.security.AuthenticatedUser;
import com.kfd.healthmenu.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;

    @PostMapping("/login")
    public ApiResponse<CurrentUserDto> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername().trim(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException ex) {
            throw new BizException("ACCOUNT_DISABLED", "该账号已停用，请联系管理员处理");
        } catch (BadCredentialsException ex) {
            throw new BizException("AUTH_FAILED", "账号或密码不正确");
        } catch (AuthenticationException ex) {
            throw new BizException("AUTH_FAILED", "登录失败，请检查账号密码");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        httpRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        accountService.recordLoginSuccess(user.getId());
        return ApiResponse.success("登录成功", accountService.getCurrentUser(user.getId()));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserDto> me(Authentication authentication) {
        AuthenticatedUser user = requireAuthenticatedUser(authentication);
        return ApiResponse.success(accountService.getCurrentUser(user.getId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication,
                                    HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ApiResponse.success("已退出登录", null);
    }

    private AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BizException("UNAUTHORIZED", "请先登录后台账号");
        }
        return user;
    }
}
