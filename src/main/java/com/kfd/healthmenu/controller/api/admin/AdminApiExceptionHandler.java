package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.api.ApiResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.kfd.healthmenu.controller.api")
public class AdminApiExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("customerId", "客户"),
            Map.entry("templateId", "模板"),
            Map.entry("menuDate", "餐单日期"),
            Map.entry("sourceText", "AI 文本"),
            Map.entry("prompt", "AI 生图提示词"),
            Map.entry("id", "ID"),
            Map.entry("name", "名称"),
            Map.entry("mealCode", "餐次编码"),
            Map.entry("mealName", "餐次名称"),
            Map.entry("itemCode", "字段编码"),
            Map.entry("itemName", "字段名称"),
            Map.entry("sectionType", "区块类型"),
            Map.entry("title", "标题"),
            Map.entry("username", "账号"),
            Map.entry("displayName", "姓名"),
            Map.entry("roleCode", "角色"),
            Map.entry("roleName", "角色名称"),
            Map.entry("permissionCodes", "权限"),
            Map.entry("status", "状态"),
            Map.entry("password", "密码")
    );

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(Exception ex) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException methodEx
                ? methodEx.getBindingResult()
                : ((BindException) ex).getBindingResult();
        String message = bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(this::resolveValidationMessage)
                .orElse("请求参数校验失败");
        return ApiResponse.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuthentication(AuthenticationException ex) {
        return ApiResponse.fail("UNAUTHORIZED", ex.getMessage() == null ? "请先登录后台账号" : ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.fail("FORBIDDEN", ex.getMessage() == null ? "当前账号没有访问该功能的权限" : ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        return ApiResponse.fail("INTERNAL_ERROR", ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }

    private String resolveValidationMessage(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage != null && !defaultMessage.isBlank() && !isGenericValidationMessage(defaultMessage)) {
            return defaultMessage;
        }

        String fieldLabel = FIELD_LABELS.getOrDefault(error.getField(), error.getField());
        String code = error.getCode();
        if ("NotNull".equals(code)) {
            return fieldLabel + "不能为空";
        }
        if ("NotBlank".equals(code) || "NotEmpty".equals(code)) {
            return fieldLabel + "不能为空";
        }
        if ("typeMismatch".equals(code)) {
            return fieldLabel + "格式不正确";
        }
        return fieldLabel + "校验失败";
    }

    private boolean isGenericValidationMessage(String message) {
        return "must not be null".equalsIgnoreCase(message)
                || "must not be blank".equalsIgnoreCase(message)
                || "must not be empty".equalsIgnoreCase(message)
                || "不能为null".equals(message)
                || "不能为空".equals(message);
    }
}
