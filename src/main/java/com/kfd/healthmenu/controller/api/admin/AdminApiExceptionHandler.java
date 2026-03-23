package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.api.ApiResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.kfd.healthmenu.controller.api")
public class AdminApiExceptionHandler {

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
                .map(error -> error.getDefaultMessage() == null ? error.getField() + "校验失败" : error.getDefaultMessage())
                .orElse("请求参数校验失败");
        return ApiResponse.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        return ApiResponse.fail("INTERNAL_ERROR", ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }
}
