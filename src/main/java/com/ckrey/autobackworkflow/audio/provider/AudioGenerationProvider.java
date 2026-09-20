package com.ckrey.autobackworkflow.audio.provider;

import java.util.Map;

public interface AudioGenerationProvider {
    String providerCode();
    ProviderCapabilities capabilities();
    AudioGenerationResult generate(AudioGenerationCommand command);
    default AudioGenerationResult query(String externalTaskId) { throw new UnsupportedOperationException("Async query is not supported"); }
    record ProviderCapabilities(boolean supportsEmotion, boolean supportsReferenceAudio, boolean supportsAsyncTask, boolean supportsStreaming, java.util.Set<String> formats) { }
    record AudioGenerationCommand(String requestId,String modelCode,String text,String voiceId,VoiceDirection direction,String format,Map<String,Object> extensions) { }
    record VoiceDirection(String instruction,Double speed,Double volume,Map<String,Object> emotion) { }
    record AudioGenerationResult(byte[] audio,String mediaType,String externalTaskId,java.util.List<String> warnings) { }
}
