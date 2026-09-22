package com.ckrey.autobackworkflow.audio.provider;

import com.ckrey.autobackworkflow.audio.config.SeedAudioProperties;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SeedAudioGenerationProvider implements AudioGenerationProvider {
    private static final Set<String> FORMATS = Set.of("wav", "mp3", "pcm", "ogg_opus");
    private static final Map<String, Set<Integer>> SAMPLE_RATES = Map.of(
            "wav", Set.of(8000, 16000, 24000, 32000, 40000, 44100, 48000),
            "pcm", Set.of(8000, 16000, 24000, 32000, 40000, 44100, 48000),
            "mp3", Set.of(8000, 16000, 24000, 32000, 44100, 48000),
            "ogg_opus", Set.of(48000));
    private static final Map<String, String> MEDIA_TYPES = Map.of(
            "wav", "audio/wav", "mp3", "audio/mpeg", "pcm", "audio/L16", "ogg_opus", "audio/ogg");
    private static final int MAX_PROMPT_LENGTH = 3_000;
    private static final int MAX_REFERENCE_BYTES = 10 * 1024 * 1024;

    private final ObjectMapper mapper;
    private final SeedAudioProperties properties;
    private final HttpClient http;

    @Autowired
    public SeedAudioGenerationProvider(ObjectMapper mapper, SeedAudioProperties properties) {
        this(mapper, properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(positive(properties.getConnectTimeoutMs(), 10_000))).build());
    }

    SeedAudioGenerationProvider(ObjectMapper mapper, SeedAudioProperties properties, HttpClient http) {
        this.mapper = mapper;
        this.properties = properties;
        this.http = http;
    }

    @Override public String providerCode() { return "seed-audio"; }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, false, false, FORMATS);
    }

    @Override
    public AudioGenerationResult generate(AudioGenerationCommand command) {
        requireApiKey();
        ValidatedRequest validated = validate(command);
        ObjectNode body = buildBody(command, validated);
        String requestId = command.requestId() == null || command.requestId().isBlank()
                ? UUID.randomUUID().toString() : command.requestId();
        SeedResponse response = send(body, requestId);
        byte[] audio;
        try {
            audio = Base64.getDecoder().decode(response.body().path("audio").asText());
        } catch (IllegalArgumentException ex) {
            throw providerError("PROVIDER_INVALID_RESPONSE", "SeedAudio 返回了无效音频数据", HttpStatus.BAD_GATEWAY);
        }
        if (audio.length == 0) {
            throw providerError("PROVIDER_INVALID_RESPONSE", "SeedAudio 未返回音频数据", HttpStatus.BAD_GATEWAY);
        }
        List<String> warnings = new ArrayList<>();
        warnings.add("seedRequestId=" + requestId);
        if (!response.logId().isBlank()) warnings.add("seedLogId=" + response.logId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestId", requestId);
        metadata.put("logId", response.logId());
        metadata.put("format", validated.format());
        metadata.put("sampleRate", validated.sampleRate());
        putNumber(metadata, "durationMs", response.body().path("duration").asDouble(-1), 1000d);
        putNumber(metadata, "originalDurationMs", response.body().path("original_duration").asDouble(-1), 1000d);
        if (response.body().has("subtitle") && !response.body().path("subtitle").isNull()) {
            metadata.put("subtitle", mapper.convertValue(response.body().path("subtitle"), Object.class));
        }
        return new AudioGenerationResult(audio, MEDIA_TYPES.get(validated.format()), null,
                List.copyOf(warnings), Map.copyOf(metadata));
    }

    private ObjectNode buildBody(AudioGenerationCommand command, ValidatedRequest validated) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", valueOrDefault(command.modelCode(), properties.getModel()));
        body.put("text_prompt", validated.prompt());
        ArrayNode references = createReferences(command.voiceId(), command.extensions());
        if (!references.isEmpty()) body.set("references", references);
        ObjectNode audio = body.putObject("audio_config");
        audio.put("format", validated.format());
        audio.put("sample_rate", validated.sampleRate());
        audio.put("speech_rate", factorToRate(command.direction() == null ? null : command.direction().speed(), "speed"));
        audio.put("loudness_rate", factorToRate(command.direction() == null ? null : command.direction().volume(), "volume"));
        audio.put("pitch_rate", intExtension(command.extensions(), "pitchRate", 0, -12, 12));
        audio.put("enable_subtitle", booleanExtension(command.extensions(), "enableSubtitle", true));
        if (booleanExtension(command.extensions(), "enableWatermark", true)) {
            ObjectNode watermark = body.putObject("watermark");
            watermark.put("aigc_watermark", true);
            ObjectNode metadata = watermark.putObject("aigc_metadata");
            metadata.put("enable", true);
            metadata.put("content_producer", "autoBackWorkFlow");
            metadata.put("produce_id", valueOrDefault(stringExtension(command.extensions(), "produceId"), command.requestId()));
            metadata.put("content_propagator", "autoBackWorkFlow");
            metadata.put("propagate_id", valueOrDefault(stringExtension(command.extensions(), "propagateId"), command.requestId()));
        }
        return body;
    }

    private ArrayNode createReferences(String voiceId, Map<String, Object> extensions) {
        Map<String, Object> ext = extensions == null ? Map.of() : extensions;
        List<String> audioData = stringList(ext.get("referenceAudioData"));
        List<String> audioUrls = stringList(ext.get("referenceAudioUrl"));
        String imageData = stringValue(ext.get("referenceImageData"));
        String imageUrl = stringValue(ext.get("referenceImageUrl"));
        int modeCount = (blank(voiceId) ? 0 : 1) + (audioData.isEmpty() ? 0 : 1) + (audioUrls.isEmpty() ? 0 : 1)
                + (blank(imageData) ? 0 : 1) + (blank(imageUrl) ? 0 : 1);
        if (modeCount > 1) throw invalidRequest("音色、参考音频和参考图片不能混用");
        if (audioData.size() > 3 || audioUrls.size() > 3) throw invalidRequest("参考音频最多 3 条");
        ArrayNode references = mapper.createArrayNode();
        if (!blank(voiceId)) references.addObject().put("speaker", voiceId);
        for (String data : audioData) {
            validateBase64(data, "参考音频");
            references.addObject().put("audio_data", data);
        }
        for (String url : audioUrls) {
            validateReferenceUrl(url);
            references.addObject().put("audio_url", url);
        }
        if (!blank(imageData)) {
            validateBase64(imageData, "参考图片");
            references.addObject().put("image_data", imageData);
        }
        if (!blank(imageUrl)) {
            validateReferenceUrl(imageUrl);
            references.addObject().put("image_url", imageUrl);
        }
        return references;
    }

    private SeedResponse send(JsonNode body, String requestId) {
        String baseUrl = valueOrDefault(properties.getBaseUrl(), "https://openspeech.bytedance.com").replaceAll("/+$", "");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v3/tts/create"))
                    .timeout(Duration.ofMillis(positive(properties.getRequestTimeoutMs(), 180_000)))
                    .header("Content-Type", "application/json")
                    .header("X-Api-Key", properties.getApiKey().trim())
                    .header("X-Api-Request-Id", requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
        } catch (Exception ex) {
            throw providerError("PROVIDER_CONFIGURATION_ERROR", "SeedAudio 服务地址配置不合法", HttpStatus.SERVICE_UNAVAILABLE);
        }
        int attempts = Math.max(1, Math.min(properties.getMaxAttempts(), 3));
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String logId = response.headers().firstValue("X-Tt-Logid").orElse("");
                if (response.statusCode() / 100 != 2) {
                    if (retryable(response.statusCode()) && attempt < attempts) {
                        backoff(attempt);
                        continue;
                    }
                    throw providerError(statusCode(response.statusCode()),
                            "SeedAudio 调用失败（HTTP " + response.statusCode() + ", logId=" + safeLogId(logId) + "）",
                            mapStatus(response.statusCode()));
                }
                JsonNode result = mapper.readTree(response.body());
                if (result.path("code").asInt(-1) != 0 || result.path("audio").asText().isBlank()) {
                    String providerCode = result.path("code").asText("unknown");
                    throw providerError("PROVIDER_REQUEST_REJECTED",
                            "SeedAudio 生成失败（code=" + providerCode + ", logId=" + safeLogId(logId) + "）",
                            HttpStatus.BAD_GATEWAY);
                }
                return new SeedResponse(result, logId);
            } catch (BizException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw providerError("PROVIDER_INTERRUPTED", "SeedAudio 调用已中断", HttpStatus.SERVICE_UNAVAILABLE);
            } catch (IOException ex) {
                if (attempt < attempts) {
                    backoff(attempt);
                    continue;
                }
                throw providerError("PROVIDER_UNAVAILABLE", "SeedAudio 服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        throw providerError("PROVIDER_UNAVAILABLE", "SeedAudio 服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private ValidatedRequest validate(AudioGenerationCommand command) {
        if (command == null || command.text() == null || command.text().isBlank()) throw invalidRequest("待生成台词不能为空");
        String instruction = command.direction() == null ? null : command.direction().instruction();
        String emotionPrompt = emotionPrompt(command.direction() == null ? Map.of() : command.direction().emotion());
        String directionPrompt = joinPrompt(instruction, emotionPrompt);
        String prompt = blank(directionPrompt) ? command.text() : directionPrompt + "\n待生成台词：" + command.text();
        if (prompt.length() > MAX_PROMPT_LENGTH) throw invalidRequest("SeedAudio 提示词不能超过 3000 字符");
        String format = valueOrDefault(command.format(), "wav").toLowerCase(Locale.ROOT);
        if (!FORMATS.contains(format)) throw invalidRequest("不支持的音频格式: " + format);
        int defaultRate = "ogg_opus".equals(format) ? 48000 : "mp3".equals(format) ? 44100 : 40000;
        int sampleRate = intExtension(command.extensions(), "sampleRate", defaultRate, 8000, 48000);
        if (!SAMPLE_RATES.get(format).contains(sampleRate)) throw invalidRequest(format + " 不支持采样率 " + sampleRate);
        factorToRate(command.direction() == null ? null : command.direction().speed(), "speed");
        factorToRate(command.direction() == null ? null : command.direction().volume(), "volume");
        return new ValidatedRequest(prompt, format, sampleRate);
    }

    private static String joinPrompt(String instruction, String emotion) {
        if (blank(instruction)) return emotion;
        if (blank(emotion)) return instruction.trim();
        return instruction.trim() + "\n" + emotion;
    }

    private static String emotionPrompt(Map<String, Object> emotion) {
        if (emotion == null || emotion.isEmpty()) return null;
        String primary = promptText(emotion.get("primary"));
        if (primary == null) return null;
        String secondary = promptText(emotion.get("secondary"));
        Double intensity = decimal(emotion.get("intensity"));
        StringBuilder prompt = new StringBuilder("情绪要求：").append(primary);
        if (secondary != null) prompt.append("，带有").append(secondary);
        if (intensity != null) prompt.append("（强度 ").append(String.format(Locale.ROOT, "%.2f", intensity)).append("）");
        prompt.append("。请在不改变台词内容的前提下自然表达。");
        return prompt.toString();
    }

    private static String promptText(Object value) {
        if (!(value instanceof String text)) return null;
        String normalized = text.replaceAll("[\\r\\n\\t]", " ").trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private static Double decimal(Object value) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) return null;
        return Math.max(0d, Math.min(1d, number.doubleValue()));
    }

    private void validateReferenceUrl(String value) {
        URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException ex) { throw invalidRequest("参考素材 URL 不合法"); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || blank(uri.getHost())) throw invalidRequest("参考素材只允许 HTTPS URL");
        boolean allowed = properties.getReferenceUrlAllowedHosts().stream()
                .anyMatch(host -> uri.getHost().equalsIgnoreCase(host) || uri.getHost().toLowerCase(Locale.ROOT).endsWith("." + host.toLowerCase(Locale.ROOT)));
        if (!allowed) throw invalidRequest("参考素材 URL 域名未加入白名单");
    }

    private static void validateBase64(String value, String field) {
        try {
            if (Base64.getDecoder().decode(value).length > MAX_REFERENCE_BYTES) throw invalidRequest(field + "不能超过 10 MB");
        } catch (IllegalArgumentException ex) {
            throw invalidRequest(field + "不是合法 Base64");
        }
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw providerError("PROVIDER_CREDENTIAL_MISSING", "SeedAudio API Key 未配置", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static int factorToRate(Double factor, String name) {
        double value = factor == null ? 1d : factor;
        if (!Double.isFinite(value) || value < 0.5d || value > 2d) throw invalidRequest(name + " 必须在 0.5 到 2.0 之间");
        return Math.max(-50, Math.min(100, (int) Math.round((value - 1d) * 100d)));
    }
    private static int intExtension(Map<String, Object> extensions, String key, int fallback, int min, int max) {
        Object value = extensions == null ? null : extensions.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw invalidRequest(key + " 必须是数字");
        int result = number.intValue();
        if (result < min || result > max) throw invalidRequest(key + " 超出允许范围");
        return result;
    }
    private static boolean booleanExtension(Map<String, Object> extensions, String key, boolean fallback) {
        Object value = extensions == null ? null : extensions.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Boolean result)) throw invalidRequest(key + " 必须是布尔值");
        return result;
    }
    private static String stringExtension(Map<String, Object> extensions, String key) {
        return extensions == null ? null : stringValue(extensions.get(key));
    }
    private static String stringValue(Object value) { return value instanceof String text && !text.isBlank() ? text : null; }
    private static List<String> stringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof String text) return text.isBlank() ? List.of() : List.of(text);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof String text) || text.isBlank()) throw invalidRequest("参考素材列表只能包含非空字符串");
                result.add(text);
            }
            return result;
        }
        throw invalidRequest("参考素材参数格式不合法");
    }
    private static void putNumber(Map<String, Object> target, String name, double value, double multiplier) {
        if (value >= 0 && Double.isFinite(value)) target.put(name, Math.round(value * multiplier));
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String valueOrDefault(String value, String fallback) { return blank(value) ? fallback : value; }
    private static int positive(int value, int fallback) { return value > 0 ? value : fallback; }
    private static boolean retryable(int status) { return status == 429 || status >= 500; }
    private static String statusCode(int status) {
        if (status == 401 || status == 403) return "PROVIDER_AUTH_FAILED";
        if (status == 429) return "PROVIDER_RATE_LIMITED";
        return status >= 500 ? "PROVIDER_UNAVAILABLE" : "PROVIDER_REQUEST_REJECTED";
    }
    private static HttpStatus mapStatus(int status) {
        if (status == 429) return HttpStatus.TOO_MANY_REQUESTS;
        return status >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    }
    private static String safeLogId(String value) { return value == null || value.isBlank() ? "-" : value.replaceAll("[^A-Za-z0-9._:-]", ""); }
    private static BizException invalidRequest(String message) { return BizException.badRequest("AUDIO_INVALID_PARAMETERS", message); }
    private static BizException providerError(String code, String message, HttpStatus status) { return new BizException(code, message, status); }
    private static void backoff(int attempt) {
        try { Thread.sleep(150L * (1L << Math.min(attempt - 1, 2))); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw providerError("PROVIDER_INTERRUPTED", "SeedAudio 调用已中断", HttpStatus.SERVICE_UNAVAILABLE); }
    }

    private record ValidatedRequest(String prompt, String format, int sampleRate) { }
    private record SeedResponse(JsonNode body, String logId) { }
}
