package com.ckrey.autobackworkflow.audio.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class AudioDtos {
    private AudioDtos() {
    }

    public record CreateTaskRequest(@NotNull Long providerId, @NotNull Long modelId, @NotEmpty List<Long> segmentIds,
                                    @Size(max = 160) String voiceId,
                                    @Size(max = 3) List<Long> referenceAudioAssetIds,
                                    Map<String, Object> parameters) {
    }
}
