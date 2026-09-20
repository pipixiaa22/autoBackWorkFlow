package com.ckrey.autobackworkflow.ai.model;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisCandidate(List<CandidateSegment> segments) {
    public record CandidateSegment(int segmentNo, String speaker, int sourceStart, int sourceEnd, String originalText,
                                   String spokenText, String subtitleText, String semanticGroup, Emotion emotion,
                                   List<String> tone, BigDecimal speed, BigDecimal volume, int pauseBeforeMs,
                                   int pauseAfterMs, List<String> emphasis, String voiceDirection, String rewriteMode,
                                   String rewriteLevel, String rewriteReason) { }
    public record Emotion(String primary, String secondary, BigDecimal intensity) { }
}
