package com.ckrey.autobackworkflow.common.exception;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BizException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BizException notFound(String code, String message) {
        return new BizException(code, message, HttpStatus.NOT_FOUND);
    }

    public static BizException conflict(String code, String message) {
        return new BizException(code, message, HttpStatus.CONFLICT);
    }

    public static BizException badRequest(String code, String message) {
        return new BizException(code, message, HttpStatus.BAD_REQUEST);
    }
}
