package com.ckrey.autobackworkflow.audio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public final class AudioDtos { private AudioDtos() { }
    public record CreateTaskRequest(@NotNull Long providerId,@NotNull Long modelId,@NotEmpty List<Long> segmentIds,@NotBlank String voiceId,Map<String,Object> parameters) { }
}
