package com.ckrey.autobackworkflow.ai.api;

import jakarta.validation.constraints.NotNull;

public final class AnalysisDtos {
    private AnalysisDtos() { }
    public record CreateAnalysisRequest(@NotNull Long skillVersionId, String rewriteMode, Long providerId, Long modelId) { }
    public record ApplyAnalysisRequest(@NotNull Integer projectVersion) { }
}
