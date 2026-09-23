package com.ckrey.autobackworkflow.ai.api;

import com.ckrey.autobackworkflow.domain.AdsAnalysisRun;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.Map;

public final class AnalysisDtos {
    private AnalysisDtos() {
    }

    public record CreateAnalysisRequest(@NotNull Long skillVersionId, String rewriteMode, Long providerId, Long modelId,
                                        Map<String, Object> localRules) {
    }

    public record ApplyAnalysisRequest(@NotNull Integer projectVersion) {
    }

    /**
     * Raw provider payloads and complete source snapshots remain server-side for audit/debugging.
     */
    public record AnalysisRunView(Long id, String runNo, Long projectId, Integer projectVersion,
                                  Long skillVersionId, Long providerId, Long modelId,
                                  String providerCodeSnapshot, String modelCodeSnapshot, String rewriteMode,
                                  String status, Object candidateResultJson, Object validationWarningsJson,
                                  String errorCode, String errorMessage, Date startedAt, Date finishedAt,
                                  Date appliedAt, Integer version, Date createdAt, Date updatedAt) {
        public static AnalysisRunView from(AdsAnalysisRun run) {
            return new AnalysisRunView(run.getId(), run.getRunNo(), run.getProjectId(), run.getProjectVersion(),
                    run.getSkillVersionId(), run.getProviderId(), run.getModelId(), run.getProviderCodeSnapshot(),
                    run.getModelCodeSnapshot(), run.getRewriteMode(), run.getStatus(), run.getCandidateResultJson(),
                    run.getValidationWarningsJson(), run.getErrorCode(), run.getErrorMessage(), run.getStartedAt(),
                    run.getFinishedAt(), run.getAppliedAt(), run.getVersion(), run.getCreatedAt(), run.getUpdatedAt());
        }
    }
}
