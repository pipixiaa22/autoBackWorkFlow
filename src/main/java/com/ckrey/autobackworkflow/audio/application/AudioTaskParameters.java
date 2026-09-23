package com.ckrey.autobackworkflow.audio.application;

import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class AudioTaskParameters {
    private AudioTaskParameters() {
    }

    static Map<String, Object> emotion(Object value, ObjectMapper mapper) {
        if (value == null) return Map.of();
        try {
            if (value instanceof String text) {
                return mapper.readValue(text, new TypeReference<>() {
                });
            }
            if (value instanceof Map<?, ?>) {
                return mapper.convertValue(value, new TypeReference<>() {
                });
            }
        } catch (Exception ex) {
            throw BizException.badRequest("AUDIO_INVALID_PARAMETERS", "emotion 参数无法解析为对象");
        }
        throw BizException.badRequest("AUDIO_INVALID_PARAMETERS", "emotion 参数必须是对象");
    }
}
