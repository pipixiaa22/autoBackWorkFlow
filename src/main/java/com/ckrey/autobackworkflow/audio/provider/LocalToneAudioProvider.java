package com.ckrey.autobackworkflow.audio.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Development provider that produces a valid silent WAV; it keeps the full task pipeline testable without credentials.
 */
@Component
public class LocalToneAudioProvider implements AudioGenerationProvider {
    @Override
    public String providerCode() {
        return "local-tone";
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(false, false, false, false, Set.of("wav"));
    }

    @Override
    public AudioGenerationResult generate(AudioGenerationCommand command) {
        int samples = Math.max(4410, Math.min(44100 * 12, command.text().codePointCount(0, command.text().length()) * 11025));
        try {
            return new AudioGenerationResult(wav(samples), "audio/wav", null, List.of("本地开发 Provider 已生成静音 WAV"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] wav(int samples) throws IOException {
        int data = samples * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + data);
        out.write("RIFF".getBytes());
        le(out, 36 + data);
        out.write("WAVEfmt ".getBytes());
        le(out, 16);
        shortLe(out, 1);
        shortLe(out, 1);
        le(out, 44100);
        le(out, 88200);
        shortLe(out, 2);
        shortLe(out, 16);
        out.write("data".getBytes());
        le(out, data);
        out.write(new byte[data]);
        return out.toByteArray();
    }

    private static void le(ByteArrayOutputStream o, int v) throws IOException {
        o.write(v);
        o.write(v >>> 8);
        o.write(v >>> 16);
        o.write(v >>> 24);
    }

    private static void shortLe(ByteArrayOutputStream o, int v) throws IOException {
        o.write(v);
        o.write(v >>> 8);
    }
}
