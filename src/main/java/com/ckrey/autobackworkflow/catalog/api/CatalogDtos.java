package com.ckrey.autobackworkflow.catalog.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record CreateProviderRequest(
            @NotBlank @Size(max = 80) String providerCode,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(max = 32) String providerType,
            @Size(max = 500) String baseUrl,
            @Size(max = 200) String credentialRef,
            @Size(max = 80) String maskedCredentialHint,
            @Min(1) @Max(3600) Integer timeoutSeconds,
            @Min(1) @Max(1000) Integer maxConcurrency,
            Map<String, Object> config,
            Boolean enabled) {
    }

    public record UpdateProviderRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(max = 32) String providerType,
            @Size(max = 500) String baseUrl,
            @Size(max = 200) String credentialRef,
            @Size(max = 80) String maskedCredentialHint,
            @Min(1) @Max(3600) Integer timeoutSeconds,
            @Min(1) @Max(1000) Integer maxConcurrency,
            Map<String, Object> config,
            Boolean enabled) {
    }

    public record CreateModelRequest(
            @NotNull Long providerId,
            @NotBlank @Size(max = 120) String modelCode,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(max = 32) String modelType,
            Map<String, Object> capabilities,
            Map<String, Object> defaultParameters,
            Boolean enabled,
            @Min(0) @Max(100000) Integer sortNo) {
    }

    public record UpdateModelRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(max = 32) String modelType,
            Map<String, Object> capabilities,
            Map<String, Object> defaultParameters,
            Boolean enabled,
            @Min(0) @Max(100000) Integer sortNo) {
    }
}
