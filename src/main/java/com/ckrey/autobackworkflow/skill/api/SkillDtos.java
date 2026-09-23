package com.ckrey.autobackworkflow.skill.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public final class SkillDtos {
    private SkillDtos() {
    }

    public record SaveSkillRequest(@NotBlank @Size(max = 160) String title,
                                   @NotBlank @Size(max = 30000) String content) {
    }

    public record SkillDetail(Long id, String title, String content, Long versionId,
                              Integer version) {
    }

    public record CreateSkillVersionRequest(@NotBlank @Size(max = 40) String versionNo,
                                            @NotBlank @Size(max = 30000) String systemPromptTemplate,
                                            Map<String, Object> inputSchema, Map<String, Object> outputSchema,
                                            Map<String, Object> splitRules, Map<String, Object> rewriteRules,
                                            Map<String, Object> modelParameters) {
    }
}
