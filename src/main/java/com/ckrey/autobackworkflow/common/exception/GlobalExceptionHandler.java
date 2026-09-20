package com.ckrey.autobackworkflow.common.exception;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BizException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException validation
                && validation.getBindingResult().getFieldError() != null
                ? validation.getBindingResult().getFieldError().getDefaultMessage() : "请求参数不合法";
        return ResponseEntity.badRequest().body(ApiResponse.failure("COMMON_VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled request failure", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("COMMON_INTERNAL_ERROR", "系统繁忙，请稍后重试"));
    }
}
