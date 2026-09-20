package com.ckrey.autobackworkflow.export.application;

import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrtGeneratorTests {
    @Test
    void generatesUtf8CompatibleSrtWithPunctuationDuration() {
        AdsDialogueSegment segment = new AdsDialogueSegment();
        segment.setSubtitleText("你好，世界！"); segment.setSpokenText("你好，世界！"); segment.setPauseAfterMs(350);
        String result = new SrtGenerator(4d, 200, 150, 300, 1200).generate(List.of(segment));
        assertTrue(result.startsWith("1\n00:00:00,000 --> 00:00:01,950"));
        assertTrue(result.contains("你好，世界！"));
    }
}
