package com.ckrey.autobackworkflow.audio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AudioTaskParametersTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsPersistedEmotionJsonIntoTheProviderDirection() {
        var emotion = AudioTaskParameters.emotion("{\"primary\":\"悲伤\",\"intensity\":0.8}", mapper);

        assertEquals("悲伤", emotion.get("primary"));
        assertEquals(0.8d, emotion.get("intensity"));
    }

    @Test
    void rejectsNonObjectEmotionValues() {
        BizException exception = assertThrows(BizException.class,
                () -> AudioTaskParameters.emotion("悲伤", mapper));

        assertEquals("AUDIO_INVALID_PARAMETERS", exception.getCode());
    }
}
