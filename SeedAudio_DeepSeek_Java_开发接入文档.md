# SeedAudio 与 DeepSeek Java 开发接入文档

> 适用项目：`autoBackWorkFlow`（Java 21、Spring Boot、Jackson）。
>
> 本文整理日期：2026-09-20。SeedAudio 部分以项目根目录的 `豆包音频接入.txt`（导出的《豆包语音_音频生成HTTP》）为准；DeepSeek 部分以其官方 API 文档为准。接口和模型可用性以控制台及官方最新文档为最终依据。

## 1. 接入目标与整体链路

本项目可将两个服务放在同一条生成链路中：

```text
原始剧本 / 对白
   │
   ├─ DeepSeek：分析、改写、生成角色/情绪/音频提示词
   │
   └─ SeedAudio：按文本、音色或参考素材生成最终音频
                         │
                         └─ 保存音频、字幕和追踪 ID，供导出/SRT 使用
```

建议保持供应商适配层隔离：DeepSeek 实现 `LlmProvider`，SeedAudio 实现 `AudioGenerationProvider`。上层业务不得持有 API Key，也不应依赖供应商特有的 JSON 字段。

## 2. 凭证与配置

### 2.1 凭证来源

| 服务 | 凭证 | 获取位置 | 请求头 |
| --- | --- | --- | --- |
| SeedAudio | API Key | 火山引擎新版控制台 → **API Key 管理** | `X-Api-Key` |
| DeepSeek | API Key | [DeepSeek Platform](https://platform.deepseek.com/api_keys) | `Authorization: Bearer <key>` |

SeedAudio 的旧式 `X-Api-App-Id + X-Api-Access-Key` 鉴权将下线；新接入只使用 `X-Api-Key`。密钥只可放在部署环境变量、密钥管理服务或 CI/CD Secret 中，禁止写入 `application.yaml`、源码、日志及数据库明文字段。

### 2.2 推荐的 `application.yaml` 配置

将以下内容加入现有 `src/main/resources/application.yaml`。默认值只包含公开地址和模型名，不包含任何密钥。

```yaml
ads:
  ai:
    seed-audio:
      base-url: ${SEED_AUDIO_BASE_URL:https://openspeech.bytedance.com}
      api-key: ${SEED_AUDIO_API_KEY:}
      model: ${SEED_AUDIO_MODEL:seed-audio-1.0}
      connect-timeout-ms: ${SEED_AUDIO_CONNECT_TIMEOUT_MS:10000}
      request-timeout-ms: ${SEED_AUDIO_REQUEST_TIMEOUT_MS:180000}
    deepseek:
      base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
      api-key: ${DEEPSEEK_API_KEY:}
      model: ${DEEPSEEK_MODEL:deepseek-flash}
      connect-timeout-ms: ${DEEPSEEK_CONNECT_TIMEOUT_MS:10000}
      request-timeout-ms: ${DEEPSEEK_REQUEST_TIMEOUT_MS:90000}
```

生产环境示例：

```bash
export SEED_AUDIO_API_KEY='***'
export DEEPSEEK_API_KEY='***'
export DEEPSEEK_MODEL='deepseek-flash'
```

SeedAudio 单次音频最长为 120 秒。因此 SeedAudio 的整体请求超时应大于 120 秒；上例使用 180 秒。连接超时和请求超时应区分配置。

## 3. SeedAudio 音频生成 HTTP API

### 3.1 接口概览

| 项目 | 值 |
| --- | --- |
| 方法和路径 | `POST https://openspeech.bytedance.com/api/v3/tts/create` |
| 调用模式 | HTTP 非流式、同步返回 |
| 当前模型 | `seed-audio-1.0` |
| 最大提示词长度 | 3,000 字符 |
| 最大原始音频时长 | 120 秒（也是计费依据） |
| 结果音频 | `audio` Base64 及临时 `url`（有效期 2 小时） |

必传请求头：

```http
Content-Type: application/json
X-Api-Key: ${SEED_AUDIO_API_KEY}
X-Api-Request-Id: <业务 Trace ID 或 UUID>
```

每次请求均生成新的 `X-Api-Request-Id`。将该 ID、响应头 `X-Tt-Logid`、本项目 `traceId` 一起写入审计日志；发生问题时可据此向火山引擎定位。

### 3.2 请求体与生成模式

最小纯文本请求：

```json
{
  "model": "seed-audio-1.0",
  "text_prompt": "深夜的电台节目。女主播用平静、温暖的普通话说：晚安，愿你有一个好梦。背景只有极轻的氛围音乐。",
  "audio_config": {
    "format": "mp3",
    "sample_rate": 44100,
    "speech_rate": 0,
    "loudness_rate": 0,
    "pitch_rate": 0,
    "enable_subtitle": true
  },
  "watermark": {
    "aigc_watermark": true,
    "aigc_metadata": {
      "enable": true,
      "content_producer": "autoBackWorkFlow",
      "produce_id": "project-123",
      "content_propagator": "autoBackWorkFlow",
      "propagate_id": "export-456"
    }
  }
}
```

`references` 按下表选择一种模式；音频参考与图片参考不可混用。

| 模式 | `references` 写法 | 关键限制 |
| --- | --- | --- |
| 纯文本 | 不传 | 由 `text_prompt` 完整描述人声、情绪、音乐、音效和时间线。 |
| 指定已有音色 | `[{"speaker":"<voice-id>"}]` | `speaker` 为豆包语音合成 2.0 音色或声音复刻音色 ID。 |
| 内嵌参考音频 | `[{"audio_data":"<Base64>"}]` | 最多 3 条；每条不超过 30 秒、10 MB；格式为 wav/mp3/pcm/ogg_opus。 |
| URL 参考音频 | `[{"audio_url":"https://..."}]` | 与 `speaker`、`audio_data` 互斥。提示词可用 `@音频1`、`@音频2` 按上传顺序引用。 |
| 内嵌参考图片 | `[{"image_data":"<Base64>"}]` | 最多 1 张、10 MB；jpeg/png/webp；不能和任意音频参考混用。 |
| URL 参考图片 | `[{"image_url":"https://..."}]` | 与 `image_data` 互斥，也不能和音频参考混用。 |

对同一条参考对象，`speaker`、`audio_data`、`audio_url` 三者互斥；`image_data`、`image_url` 互斥。图片参考场景下，`text_prompt` 可只写待合成的文本。

### 3.3 输出音频参数

`audio_config` 为可选对象；不传时格式默认 `wav`。参数如下。

| 参数 | 默认值 | 合法值/说明 |
| --- | --- | --- |
| `format` | `wav` | `wav`、`mp3`、`pcm`、`ogg_opus`。 |
| `sample_rate` | wav/pcm: 40000；mp3: 44100 | wav/pcm 可用 8000/16000/24000/32000/40000/44100/48000；mp3 可用 8000/16000/24000/32000/44100/48000；ogg_opus 仅 48000。 |
| `speech_rate` | 0 | -50 至 100；-50 = 0.5 倍速，100 = 2 倍速。 |
| `loudness_rate` | 0 | -50 至 100；-50 = 0.5 倍音量，100 = 2 倍音量。 |
| `pitch_rate` | 0 | -12 至 12。 |
| `enable_subtitle` | false | `true` 时响应含句/词级时间戳。 |

对项目当前 `AudioGenerationProvider.VoiceDirection`，若 `speed`、`volume` 使用倍率（0.5 至 2.0），可转换为 API 整数：`round((倍率 - 1) * 100)`，再钳制至 `[-50, 100]`。

### 3.4 响应处理

成功与失败均应先检查 HTTP 状态，再解析 JSON：

```json
{
  "code": 0,
  "message": "success",
  "audio": "<Base64 音频数据>",
  "duration": 3.4,
  "original_duration": 3.2,
  "url": "<有效 2 小时的临时音频 URL>",
  "subtitle": {
    "text": "...",
    "sentences": [
      {
        "start_time": 0,
        "end_time": 1200,
        "text": "...",
        "words": [{"start_time": 0, "end_time": 200, "text": "..."}]
      }
    ]
  }
}
```

- `code` 非成功值时，以 `code`、`message`、`X-Tt-Logid` 构造受控异常；不要把完整请求体或密钥写到日志。
- 优先立即解码 `audio` 并存入项目资产存储，不能把 2 小时临时 `url` 当作长期素材地址。
- `original_duration` 是计费时长；`duration` 是变速/后处理后的播放时长。
- 字幕时间为相对音频起点的毫秒偏移，正好可转入本项目的 `AdsDialogueSegment`/SRT 导出链路。

### 3.5 Java 21 最小调用示例

项目已含 Jackson；以下示例只使用 JDK `HttpClient` 与现有 Jackson，无需新增 Maven 依赖。

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

public final class SeedAudioClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final String apiKey;

    public SeedAudioClient(ObjectMapper mapper, String apiKey) {
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    public byte[] generateMp3(String prompt, String speaker) throws Exception {
        var body = mapper.createObjectNode();
        body.put("model", "seed-audio-1.0");
        body.put("text_prompt", prompt);
        if (speaker != null && !speaker.isBlank()) {
            body.putArray("references").addObject().put("speaker", speaker);
        }
        var audio = body.putObject("audio_config");
        audio.put("format", "mp3");
        audio.put("sample_rate", 44100);
        audio.put("enable_subtitle", true);

        var request = HttpRequest.newBuilder(URI.create(
                        "https://openspeech.bytedance.com/api/v3/tts/create"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .header("X-Api-Request-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String logId = response.headers().firstValue("X-Tt-Logid").orElse("-");
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("SeedAudio HTTP " + response.statusCode() + ", logId=" + logId);
        }
        JsonNode result = mapper.readTree(response.body());
        if (result.path("code").asInt(-1) != 0 || result.path("audio").asText().isBlank()) {
            throw new IllegalStateException("SeedAudio failed: " + result.path("message").asText() + ", logId=" + logId);
        }
        return Base64.getDecoder().decode(result.path("audio").asText());
    }

    public static void save(Path destination, byte[] bytes) throws Exception {
        Files.createDirectories(destination.getParent());
        Files.write(destination, bytes);
    }
}
```

实际接入时，请将示例中的固定 URL、模型和超时替换为 `ads.ai.seed-audio` 配置，并把响应中的 `subtitle`、`duration`、`original_duration`、`X-Tt-Logid` 落库或存入任务审计上下文。

## 4. DeepSeek 对话 API

### 4.1 接口概览

DeepSeek 使用与 OpenAI 兼容的接口格式。本项目用原生 HTTP 即可，也可在后续按需替换为 OpenAI Java SDK。

| 项目 | 值 |
| --- | --- |
| Base URL | `https://api.deepseek.com` |
| 对话路径 | `POST /chat/completions` |
| 完整地址 | `https://api.deepseek.com/chat/completions` |
| 默认推荐模型 | `deepseek-flash` |
| 另一个当前文档列出的模型 | `deepseek-v4-pro` |
| 鉴权 | `Authorization: Bearer ${DEEPSEEK_API_KEY}` |

官方文档标明旧模型别名 `deepseek-v4-flash`、`deepseek-v4-flash-vision-exp` 仍可能被路由，但已下线；新代码统一使用 `deepseek-flash`，不要依赖旧名称。

### 4.2 非流式请求与响应

推荐先使用非流式，便于本项目的分析任务获得完整 JSON 后再进行校验：

```json
{
  "model": "deepseek-flash",
  "messages": [
    {"role": "system", "content": "你是短剧音频制作助手。只输出合法 JSON。"},
    {"role": "user", "content": "将这句台词整理为音频生成提示词：今晚别走。"}
  ],
  "thinking": {"type": "enabled"},
  "reasoning_effort": "high",
  "stream": false
}
```

对应 HTTP 头：

```http
Content-Type: application/json
Authorization: Bearer ${DEEPSEEK_API_KEY}
```

从返回 JSON 中读取 `choices[0].message.content` 作为业务结果。对用于驱动音频任务的结构化内容，必须做以下校验后再下发 SeedAudio：JSON 可解析、字段白名单、提示词长度不超过 3000、音频控制参数在合法范围内。模型输出不是可信配置输入。

`stream: true` 时接口使用流式返回，适合聊天 UI；后台批量分析任务通常使用 `false`。若启用流式，必须按 Server-Sent Events 的 `data:` 事件逐段累积，并处理结束标识后再解析最终内容。

### 4.3 Java 21 最小调用示例

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class DeepSeekClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String baseUrl;

    public DeepSeekClient(ObjectMapper mapper, String apiKey, String baseUrl) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String chat(String systemPrompt, String userPrompt) throws Exception {
        var body = mapper.createObjectNode();
        body.put("model", "deepseek-flash");
        body.put("stream", false);
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        var request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("DeepSeek HTTP " + response.statusCode());
        }
        JsonNode root = mapper.readTree(response.body());
        JsonNode content = root.at("/choices/0/message/content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("DeepSeek response has no choices[0].message.content");
        }
        return content.asText();
    }
}
```

如不需要推理过程，可省略 `thinking` 与 `reasoning_effort`；如启用，使用官方示例中的 `thinking: {"type":"enabled"}`、`reasoning_effort: "high"`，并以实际模型能力和成本策略为准。

## 5. 与当前工程的落地映射

### 5.1 SeedAudio Provider

新增 `SeedAudioGenerationProvider implements AudioGenerationProvider`，建议映射如下：

| 现有命令字段 | SeedAudio 字段 | 规则 |
| --- | --- | --- |
| `modelCode` | `model` | 空值时使用配置的 `seed-audio-1.0`。 |
| `text` | `text_prompt` | 先校验不为空且不超过 3,000 字符。 |
| `voiceId` | `references[0].speaker` | 有值时使用指定音色。 |
| `direction.speed` | `audio_config.speech_rate` | 按本文 3.3 的倍率换算。 |
| `direction.volume` | `audio_config.loudness_rate` | 按本文 3.3 的倍率换算。 |
| `direction.instruction` | 合并入 `text_prompt` | 不可覆盖用户台词；明确规定语气/情绪/背景。 |
| `format` | `audio_config.format` | 白名单校验 wav/mp3/pcm/ogg_opus。 |
| `extensions.enableSubtitle` | `audio_config.enable_subtitle` | 仅布尔值。 |
| `requestId` | `X-Api-Request-Id` | 为空时生成 UUID。 |

`ProviderCapabilities` 可声明为：支持参考音频、不支持异步任务、不支持该 HTTP 接口的流式输出，格式集合为 `wav`、`mp3`、`pcm`、`ogg_opus`。下载/持久化成功后构造 `AudioGenerationResult(audio, mediaType, null, warnings)`；不要把临时 `url` 当作 `externalTaskId`。

### 5.2 DeepSeek LLM Provider

新增 `DeepSeekLlmProvider implements LlmProvider`：

1. 使用 `LlmAnalysisCommand` 组成固定 system prompt 与用户上下文。
2. 要求模型仅返回与 `AnalysisCandidate` 对齐的 JSON。
3. 使用 Jackson 反序列化，并对角色、情绪、速度、音量、时间线、提示词长度做服务端校验。
4. 校验失败时返回可诊断的业务异常或受控降级，不能直接相信模型返回结果。

## 6. 稳定性、安全与可观测性

- **重试**：仅对连接失败、超时、HTTP 5xx 或明确可重试的限流错误进行有限重试（建议 2 次，指数退避加随机抖动）。不要对鉴权失败、参数校验失败或内容违规错误重试。
- **幂等与重复计费**：音频生成可能产生费用。请求超时后不能盲目无限重试；以业务任务 ID、请求哈希、`X-Api-Request-Id` 记录一次提交意图，并人工/业务规则决定是否再次生成。
- **日志脱敏**：永不记录两个 API Key、`Authorization`、`X-Api-Key`、完整参考音频 Base64、完整用户隐私文本。记录供应商名、模型、耗时、HTTP 状态、供应商错误码、`X-Tt-Logid` 和本地 traceId 即可。
- **输入限制**：调用前做字符数、音频条数/时长/文件大小、图片大小/格式、输出格式、倍率和互斥字段校验，避免将无效请求发送至供应商。
- **资产留存**：将实际音频写入 `ads.storage.root` 管理的长期存储；可选保存字幕 JSON。临时 `url` 到期后不可用于回放或导出。
- **访问控制**：参考音频/图片 URL 不应指向内网地址；如允许 URL 参考，应在业务侧做域名白名单和文件大小限制，降低 SSRF 与超大文件风险。

## 7. 上线前验收清单

- [ ] 两个密钥均只通过环境变量或密钥管理服务注入，日志扫描无泄漏。
- [ ] 使用 SeedAudio 纯文本请求成功，生成结果能播放并已持久化。
- [ ] 使用一个 `speaker` 音色成功；再分别验证参考音频与参考图片，且互斥校验生效。
- [ ] 验证 mp3 44100、wav 40000、ogg_opus 48000 及非法采样率被拦截。
- [ ] `enable_subtitle=true` 时字幕毫秒时间戳可正确生成 SRT。
- [ ] 人为触发非 2xx、业务 `code` 失败与超时，确认错误日志同时含本地 traceId 和 SeedAudio `X-Tt-Logid`（如有）。
- [ ] DeepSeek 可返回并通过 JSON 校验；无效模型输出不会直接调用 SeedAudio。
- [ ] 对网络失败的有限重试、重复生成防护和任务状态流转有自动化测试。

## 8. 官方参考

- [豆包语音 - 音频生成 HTTP](https://docs.volcengine.com/docs/DoubaoVoice/audio-generation-http?lang=zh)（本地导出依据：`豆包音频接入.txt`）
- [DeepSeek API 文档](https://api-docs.deepseek.com/zh-cn/)
- [DeepSeek 首次调用 API / Chat Completions](https://api-docs.deepseek.com/zh-cn/)
