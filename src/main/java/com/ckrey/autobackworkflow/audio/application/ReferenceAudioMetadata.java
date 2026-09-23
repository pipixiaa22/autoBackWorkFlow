package com.ckrey.autobackworkflow.audio.application;

import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

final class ReferenceAudioMetadata {
    private ReferenceAudioMetadata() {
    }

    static Metadata read(Path path) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            long frames = stream.getFrameLength();
            var format = stream.getFormat();
            float frameRate = format.getFrameRate();
            long duration = frames > 0 && frameRate > 0 ? Math.round(frames * 1000d / frameRate) : 0L;
            int sampleRate = format.getSampleRate() > 0 ? Math.round(format.getSampleRate()) : 0;
            int bitDepth = Math.max(format.getSampleSizeInBits(), 0);
            int channels = Math.max(format.getChannels(), 0);
            return new Metadata(duration, sampleRate, bitDepth, channels);
        } catch (Exception ignored) {
            // Java's standard audio readers do not decode every allowed reference format.
        }
        return new Metadata(0L, 0, 0, 0);
    }

    record Metadata(long durationMs, int sampleRateHz, int bitDepth, int channels) {
    }
}
