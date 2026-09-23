package com.ckrey.autobackworkflow.audio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;

class ReferenceAudioMetadataTests {
    @Test
    void readsWavDurationForRequiredReferenceAssetMetadata() throws Exception {
        Path file = Files.createTempFile("reference-audio-", ".wav");
        try {
            AudioFormat format = new AudioFormat(8_000, 16, 1, true, false);
            try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(new byte[16_000]), format, 8_000)) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file.toFile());
            }
            ReferenceAudioMetadata.Metadata metadata = ReferenceAudioMetadata.read(file);
            assertEquals(1_000L, metadata.durationMs());
            assertEquals(8_000, metadata.sampleRateHz());
            assertEquals(16, metadata.bitDepth());
            assertEquals(1, metadata.channels());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
