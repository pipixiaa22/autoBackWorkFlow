package com.ckrey.autobackworkflow.ai.llm;

import com.ckrey.autobackworkflow.ai.config.DeepSeekProperties;
import com.ckrey.autobackworkflow.ai.model.AnalysisCandidate;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekLlmProvider implements LlmProvider {
    private static final int MAX_DIALOGUE_LENGTH = 100_000;
    private static final int MAX_SEGMENTS = 500;
    private static final BigDecimal MIN_FACTOR = new BigDecimal("0.5");
    private static final BigDecimal MAX_FACTOR = new BigDecimal("2.0");
    private static final Set<String> REWRITE_MODES = Set.of("STRICT", "LIGHT", "PERFORMANCE");
    private static final String OUTPUT_RULES = """
            你必须只输出一个合法 JSON 对象，不要输出 Markdown、解释或代码围栏。JSON 顶层只能包含 segments。
            segments 中每项必须严格包含：segmentNo,speaker,sourceStart,sourceEnd,originalText,spokenText,
            subtitleText,semanticGroup,emotion,tone,speed,volume,pauseBeforeMs,pauseAfterMs,emphasis,
            voiceDirection,rewriteMode,rewriteLevel,rewriteReason。emotion 必须包含 primary,secondary,intensity。
            sourceStart/sourceEnd 是原始台词 Java UTF-16 字符串中的左闭右开索引，originalText 必须等于该区间原文。
            segmentNo 从 1 连续递增；speed、volume 范围 0.5 到 2.0；emotion.intensity 范围 0 到 1；停顿使用非负毫秒。
            为 SeedAudio 合成优化：spokenText 最多 2400 个字符；emotion.primary 使用中文情绪词（如平静、喜悦、悲伤、愤怒、紧张、恐惧、惊讶、温柔）；
            voiceDirection 使用不超过 400 字的中文可执行描述，包含语气、情绪和强度，但不得改写 spokenText。
            不得添加 JSON Schema 之外的字段。
            """;

    private final ObjectMapper mapper;
    private final DeepSeekProperties properties;
    private final HttpClient http;

    @Autowired
    public DeepSeekLlmProvider(ObjectMapper mapper, DeepSeekProperties properties) {
        this(mapper, properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(positive(properties.getConnectTimeoutMs(), 10_000))).build());
    }

    DeepSeekLlmProvider(ObjectMapper mapper, DeepSeekProperties properties, HttpClient http) {
        this.mapper = mapper;
        this.properties = properties;
        this.http = http;
    }

    @Override
    public String providerCode() {
        return "deepseek";
    }

    @Override
    public AnalysisCandidate analyse(LlmAnalysisCommand command) {
        validateCommand(command);
        requireApiKey();
        JsonNode response = send(buildBody(command));
        JsonNode content = response.at("/choices/0/message/content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw providerError("PROVIDER_INVALID_RESPONSE", "DeepSeek 响应缺少分析内容", HttpStatus.BAD_GATEWAY);
        }
        try {
            AnalysisCandidate candidate = mapper.readValue(content.asText(), AnalysisCandidate.class);
            validateCandidate(candidate, command);
            return candidate;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw providerError("SKILL_INVALID_OUTPUT", "DeepSeek 未返回合法的分析 JSON", HttpStatus.BAD_GATEWAY);
        }
    }

    private JsonNode buildBody(LlmAnalysisCommand command) {
        var body = mapper.createObjectNode();
        body.put("model", valueOrDefault(command.modelCode(), properties.getModel()));
        body.put("stream", false);
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system")
                .put("content", valueOrDefault(command.prompt(), "你是中文短剧音频制作助手。") + "\n\n" + OUTPUT_RULES);
        var context = mapper.createObjectNode();
        context.put("rewriteMode", valueOrDefault(command.rewriteMode(), "STRICT"));
        context.put("storyBackground", nullToEmpty(command.storyBackground()));
        context.put("originalDialogue", command.originalDialogue());
        messages.addObject().put("role", "user").put("content", context.toString());
        return body;
    }

    private JsonNode send(JsonNode body) {
        String baseUrl = valueOrDefault(properties.getBaseUrl(), "https://api.deepseek.com").replaceAll("/+$", "");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofMillis(positive(properties.getRequestTimeoutMs(), 90_000)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
        } catch (Exception ex) {
            throw providerError("PROVIDER_CONFIGURATION_ERROR", "DeepSeek 服务地址配置不合法", HttpStatus.SERVICE_UNAVAILABLE);
        }
        int attempts = Math.max(1, Math.min(properties.getMaxAttempts(), 3));
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 == 2) return mapper.readTree(response.body());
                if (retryable(response.statusCode()) && attempt < attempts) {
                    backoff(attempt);
                    continue;
                }
                throw providerError(statusCode(response.statusCode()),
                        "DeepSeek 调用失败（HTTP " + response.statusCode() + "）", mapStatus(response.statusCode()));
            } catch (BizException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw providerError("PROVIDER_INTERRUPTED", "DeepSeek 调用已中断", HttpStatus.SERVICE_UNAVAILABLE);
            } catch (IOException ex) {
                if (attempt < attempts) {
                    backoff(attempt);
                    continue;
                }
                throw providerError("PROVIDER_UNAVAILABLE", "DeepSeek 服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        throw providerError("PROVIDER_UNAVAILABLE", "DeepSeek 服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void validateCommand(LlmAnalysisCommand command) {
        if (command == null || command.originalDialogue() == null || command.originalDialogue().isBlank()) {
            throw BizException.badRequest("SKILL_INVALID_INPUT", "原始台词不能为空");
        }
        if (command.originalDialogue().length() > MAX_DIALOGUE_LENGTH) {
            throw BizException.badRequest("SKILL_INVALID_INPUT", "原始台词过长");
        }
        String rewriteMode = valueOrDefault(command.rewriteMode(), "STRICT");
        if (!REWRITE_MODES.contains(rewriteMode)) {
            throw BizException.badRequest("SKILL_INVALID_INPUT", "不支持的改写模式");
        }
    }

    private void validateCandidate(AnalysisCandidate candidate, LlmAnalysisCommand command) {
        if (candidate == null || candidate.segments() == null || candidate.segments().isEmpty()
                || candidate.segments().size() > MAX_SEGMENTS) {
            invalidOutput("分析结果分段数量不合法");
        }
        int previousEnd = 0;
        List<AnalysisCandidate.CandidateSegment> segments = candidate.segments();
        for (int index = 0; index < segments.size(); index++) {
            AnalysisCandidate.CandidateSegment item = segments.get(index);
            if (item == null || item.segmentNo() != index + 1) invalidOutput("分段编号必须从 1 连续递增");
            if (blank(item.speaker()) || blank(item.originalText()) || blank(item.spokenText())
                    || blank(item.subtitleText()) || blank(item.voiceDirection()))
                invalidOutput("分析结果存在空的必填字段");
            if (item.speaker().length() > 120 || item.voiceDirection().length() > 400
                    || item.spokenText().length() > 2_400) invalidOutput("分析结果字段长度超出 SeedAudio 限制");
            if (item.sourceStart() < previousEnd || item.sourceEnd() <= item.sourceStart()
                    || item.sourceEnd() > command.originalDialogue().length()) invalidOutput("原文索引不合法或重叠");
            if (!command.originalDialogue().substring(item.sourceStart(), item.sourceEnd()).equals(item.originalText()))
                invalidOutput("分段原文与原文索引不一致");
            requireRange(item.speed(), MIN_FACTOR, MAX_FACTOR, "speed");
            requireRange(item.volume(), MIN_FACTOR, MAX_FACTOR, "volume");
            if (item.emotion() == null || blank(item.emotion().primary())) invalidOutput("emotion 不完整");
            requireRange(item.emotion().intensity(), BigDecimal.ZERO, BigDecimal.ONE, "emotion.intensity");
            if (item.pauseBeforeMs() < 0 || item.pauseAfterMs() < 0
                    || item.pauseBeforeMs() > 120_000 || item.pauseAfterMs() > 120_000) invalidOutput("停顿时长不合法");
            if (!valueOrDefault(command.rewriteMode(), "STRICT").equals(item.rewriteMode()))
                invalidOutput("改写模式不一致");
            previousEnd = item.sourceEnd();
        }
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw providerError("PROVIDER_CREDENTIAL_MISSING", "DeepSeek API Key 未配置", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static void requireRange(BigDecimal value, BigDecimal min, BigDecimal max, String name) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0)
            invalidOutput(name + " 超出允许范围");
    }

    private static void invalidOutput(String message) {
        throw providerError("SKILL_INVALID_OUTPUT", message, HttpStatus.BAD_GATEWAY);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static boolean retryable(int status) {
        return status == 429 || status >= 500;
    }

    private static String statusCode(int status) {
        if (status == 401 || status == 403) return "PROVIDER_AUTH_FAILED";
        if (status == 429) return "PROVIDER_RATE_LIMITED";
        return status >= 500 ? "PROVIDER_UNAVAILABLE" : "PROVIDER_REQUEST_REJECTED";
    }

    private static HttpStatus mapStatus(int status) {
        if (status == 429) return HttpStatus.TOO_MANY_REQUESTS;
        if (status == 401 || status == 403) return HttpStatus.BAD_GATEWAY;
        return status >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    }

    private static BizException providerError(String code, String message, HttpStatus status) {
        return new BizException(code, message, status);
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(100L * (1L << Math.min(attempt - 1, 2)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw providerError("PROVIDER_INTERRUPTED", "DeepSeek 调用已中断", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
