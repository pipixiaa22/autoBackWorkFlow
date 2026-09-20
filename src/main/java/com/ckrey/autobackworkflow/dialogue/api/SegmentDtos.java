package com.ckrey.autobackworkflow.dialogue.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class SegmentDtos {
    private SegmentDtos() { }
    public record UpdateSegmentRequest(
            @NotNull Integer version, @Size(max = 5000) String spokenText, @Size(max = 5000) String subtitleText,
            String speakerName, String semanticGroup, Map<String, Object> emotion, List<String> tone,
            @Min(0) @Max(3000) Integer pauseBeforeMs, @Min(0) @Max(10000) Integer pauseAfterMs,
            BigDecimal speed, BigDecimal volume, List<String> emphasis, @Size(max = 2000) String voiceDirection,
            String rewriteMode, String rewriteLevel, @Size(max = 1000) String rewriteReason) { }
    public record SplitSegmentRequest(@NotNull Integer version, @NotNull @Min(1) Integer splitOffset) { }
    public record MergeSegmentsRequest(@NotNull Long firstSegmentId, @NotNull Long secondSegmentId,
                                       @NotNull Integer firstVersion, @NotNull Integer secondVersion) { }
    public record ReorderSegmentsRequest(@NotNull Integer projectVersion, @NotEmpty List<Long> segmentIds) { }
}
