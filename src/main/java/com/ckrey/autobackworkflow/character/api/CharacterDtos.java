package com.ckrey.autobackworkflow.character.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public final class CharacterDtos {
    private CharacterDtos() {
    }

    public record CreateCharacterRequest(
            @NotBlank @Size(max = 80) String characterCode,
            @NotBlank @Size(max = 120) String name,
            String description,
            Long defaultProviderId,
            Long defaultModelId,
            @Size(max = 160) String defaultVoiceId,
            Map<String, Object> voiceConfig,
            @Min(0) @Max(100000) Integer sortNo) {
    }

    public record UpdateCharacterRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 120) String name,
            String description,
            Long defaultProviderId,
            Long defaultModelId,
            @Size(max = 160) String defaultVoiceId,
            Map<String, Object> voiceConfig,
            @Min(0) @Max(100000) Integer sortNo) {
    }
}
