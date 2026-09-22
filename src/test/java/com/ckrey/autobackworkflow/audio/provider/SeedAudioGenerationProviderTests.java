package com.ckrey.autobackworkflow.audio.provider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ckrey.autobackworkflow.audio.config.SeedAudioProperties;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeedAudioGenerationProviderTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> requestId = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v3/tts/create", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Api-Request-Id"));
            var response = mapper.createObjectNode();
            response.put("code", 0);
            response.put("message", "success");
            response.put("audio", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}));
            response.put("duration", 1.25);
            response.put("original_duration", 1.5);
            response.putObject("subtitle").put("text", "晚安");
            byte[] bytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-Tt-Logid", "log-123");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() { server.stop(0); }

    @Test
    void mapsVoiceDirectionAndPersistsProviderMetadata() throws Exception {
        SeedAudioGenerationProvider provider = provider();
        var command = new AudioGenerationProvider.AudioGenerationCommand(
                "request-123", "seed-audio-1.0", "晚安", "speaker-1",
                new AudioGenerationProvider.VoiceDirection("温暖、平静", 0.8, 1.2,
                        Map.of("primary", "悲伤", "secondary", "克制", "intensity", 0.8)),
                "mp3", Map.of("sampleRate", 44100, "enableSubtitle", true));

        var result = provider.generate(command);

        assertArrayEquals(new byte[]{1, 2, 3, 4}, result.audio());
        assertEquals("audio/mpeg", result.mediaType());
        assertEquals(1250L, result.metadata().get("durationMs"));
        assertEquals("log-123", result.metadata().get("logId"));
        assertEquals("test-seed-key", apiKey.get());
        assertEquals("request-123", requestId.get());
        JsonNode body = mapper.readTree(requestBody.get());
        assertEquals("speaker-1", body.at("/references/0/speaker").asText());
        assertEquals(-20, body.at("/audio_config/speech_rate").asInt());
        assertEquals(20, body.at("/audio_config/loudness_rate").asInt());
        assertEquals(44100, body.at("/audio_config/sample_rate").asInt());
        assertTrue(body.path("text_prompt").asText().contains("待生成台词：晚安"));
        assertTrue(body.path("text_prompt").asText().contains("情绪要求：悲伤，带有克制（强度 0.80）"));
    }

    @Test
    void rejectsInvalidFormatBeforeSendingRequest() {
        var command = new AudioGenerationProvider.AudioGenerationCommand(
                "request-123", null, "晚安", "speaker-1",
                new AudioGenerationProvider.VoiceDirection(null, 1d, 1d, Map.of()),
                "flac", Map.of());
        BizException exception = assertThrows(BizException.class, () -> provider().generate(command));
        assertEquals("AUDIO_INVALID_PARAMETERS", exception.getCode());
    }

    @Test
    void allowsAutomaticVoiceGenerationWithoutSpeakerOrReferenceAudio() throws Exception {
        var command = new AudioGenerationProvider.AudioGenerationCommand(
                "request-auto", "seed-audio-1.0", "我会找到你的。", "",
                new AudioGenerationProvider.VoiceDirection("坚定、克制", 1d, 1d, Map.of()), "wav", Map.of());

        provider().generate(command);

        JsonNode body = mapper.readTree(requestBody.get());
        assertTrue(body.path("references").isMissingNode());
        assertTrue(body.path("text_prompt").asText().contains("我会找到你的。"));
    }

    private SeedAudioGenerationProvider provider() {
        SeedAudioProperties properties = new SeedAudioProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-seed-key");
        properties.setMaxAttempts(1);
        return new SeedAudioGenerationProvider(mapper, properties);
    }
}
