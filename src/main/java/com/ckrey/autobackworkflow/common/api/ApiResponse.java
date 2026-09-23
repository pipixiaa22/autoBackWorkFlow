package com.ckrey.autobackworkflow.common.api;

import java.util.Date;

import org.slf4j.MDC;

/**
 * Stable envelope used by every JSON API response.
 */
public record ApiResponse<T>(boolean success, String code, String message, T data,
                             String traceId, Date timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data, currentTraceId(), new Date());
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, currentTraceId(), new Date());
    }

    private static String currentTraceId() {
        String value = MDC.get("traceId");
        return value == null ? "" : value;
    }
}
