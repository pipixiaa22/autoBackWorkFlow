package com.ckrey.autobackworkflow.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ckrey.autobackworkflow.ai.config.DeepSeekProperties;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeepSeekLlmProviderTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> modelContent = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            var response = mapper.createObjectNode();
            response.putArray("choices").addObject().putObject("message").put("content", modelContent.get());
            byte[] bytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() { server.stop(0); }

    @Test
    void mapsRequestAndParsesValidatedCandidate() throws Exception {
        modelContent.set(candidateJson("1.0"));
        DeepSeekLlmProvider provider = provider();

        var result = provider.analyse(new LlmProvider.LlmAnalysisCommand(
                "夜晚", "你好。", "STRICT", "测试 Skill", "deepseek-flash"));

        assertEquals(1, result.segments().size());
        assertEquals("你好。", result.segments().getFirst().originalText());
        assertEquals("Bearer test-deepseek-key", authorization.get());
        JsonNode request = mapper.readTree(requestBody.get());
        assertEquals("deepseek-flash", request.path("model").asText());
        assertEquals(false, request.path("stream").asBoolean());
        assertTrue(request.at("/messages/0/content").asText().contains("只输出一个合法 JSON"));
        assertTrue(request.at("/messages/1/content").asText().contains("你好。"));
    }

    @Test
    void rejectsOutOfRangeModelOutput() throws Exception {
        modelContent.set(candidateJson("3.0"));
        BizException exception = assertThrows(BizException.class, () -> provider().analyse(
                new LlmProvider.LlmAnalysisCommand("", "你好。", "STRICT", "prompt", "deepseek-flash")));
        assertEquals("SKILL_INVALID_OUTPUT", exception.getCode());
    }

    private DeepSeekLlmProvider provider() {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-deepseek-key");
        properties.setMaxAttempts(1);
        return new DeepSeekLlmProvider(mapper, properties);
    }

    private String candidateJson(String speed) throws Exception {
        var root = mapper.createObjectNode();
        var segment = root.putArray("segments").addObject();
        segment.put("segmentNo", 1);
        segment.put("speaker", "角色A");
        segment.put("sourceStart", 0);
        segment.put("sourceEnd", 3);
        segment.put("originalText", "你好。");
        segment.put("spokenText", "你好。");
        segment.put("subtitleText", "你好。");
        segment.put("semanticGroup", "问候");
        segment.putObject("emotion").put("primary", "neutral").putNull("secondary").put("intensity", 0.5);
        segment.putArray("tone").add("natural");
        segment.put("speed", speed);
        segment.put("volume", 1.0);
        segment.put("pauseBeforeMs", 0);
        segment.put("pauseAfterMs", 300);
        segment.putArray("emphasis");
        segment.put("voiceDirection", "自然表达");
        segment.put("rewriteMode", "STRICT");
        segment.put("rewriteLevel", "NONE");
        segment.put("rewriteReason", "未改写");
        return mapper.writeValueAsString(root);
    }
}
