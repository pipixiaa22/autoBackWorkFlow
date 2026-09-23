# Audio Dialogue Studio 前端接口文档

> 基于当前后端控制器与 DTO 源码生成。Base URL：`/api/v1`；所有 JSON 请求使用 `Content-Type: application/json`。当前未实现鉴权，因此没有认证请求头要求。

## 通用约定

### 统一 JSON 响应

除文件下载接口外，响应均为以下信封结构。`timestamp` 为 Java `Date` 默认序列化值（毫秒时间戳）。

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "可用于排查问题的链路 ID",
  "timestamp": 1780000000000
}
```

失败时 `data` 为 `null`。常用 HTTP 状态：`400` 参数或状态不合法、`404` 资源不存在、`409` 乐观锁/业务冲突、`500` 未预期错误。

### 并发与幂等

- 项目、台词分段的编辑请求必须携带当前资源的 `version`；冲突返回 `409`，前端应刷新后提示用户处理。
- 创建音频任务、导出 SRT、导出素材包支持可选 `Idempotency-Key` 请求头。对同一项目（导出还区分导出类型）重复使用同一非空键会直接返回已有记录。
- 时间字段均为毫秒时间戳；数据库 JSON 字段在响应中可能是 JSON 对象/数组，也可能是 JSON 字符串，前端应按实际返回兼容解析。

## 建议流程

1. 创建项目 → 查询 Skill 与版本 → 创建 AI 分析。
2. 分析成功后应用结果，得到正式台词分段；可进行编辑、拆分、合并、排序。
3. 查询音频 Provider/模型，创建音频任务，并通过 SSE 接收任务状态直至终态。
4. 创建 SRT 或素材包导出，获取记录后下载文件。

## 项目

### 创建项目

`POST /projects`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 项目名，1–120 字符 |
| storyBackground | string | 否 | 剧情背景，最多 20,000 字符；空白会转为 `null` |
| originalDialogue | string | 是 | 原始台词，最多 100,000 字符 |
| outputMode | string | 否 | 输出模式；未传默认 `JIAN_YING_TTS` |
| defaultSkillVersionId | number | 否 | 默认 Skill 版本 ID |

```json
{"name":"第一集","storyBackground":"都市悬疑","originalDialogue":"你终于来了。","outputMode":"JIAN_YING_TTS","defaultSkillVersionId":1}
```

返回 `data: Project`，新项目初始 `status` 为 `DRAFT`、`version` 为 `1`。

### 查询项目详情

`GET /projects/{id}`

返回 `data: ProjectView`，其中 `segments` 已按 `segmentNo` 升序包含当前有效台词分段。

### 更新项目

`PUT /projects/{id}`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 1–120 字符 |
| storyBackground | string | 否 | 最多 20,000 字符；变更后会将 `analysisStale` 标记为 `true` |
| outputMode | string | 否 | 未传时保持原值 |
| version | integer | 是 | 当前项目版本 |

成功后版本自动加 1；版本不符返回 `PROJECT_VERSION_CONFLICT`（409）。

### 回收站

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| DELETE | `/projects/{id}?version=1` | 以乐观锁将项目放入回收站；关联分段、任务和导出数据保留。 |
| GET | `/projects/trash?cursor=&limit=20` | 返回 `{ items: Project[], nextCursor, hasMore }`，按删除时间倒序。 |
| POST | `/projects/{id}/restore` | 从回收站恢复项目；成功后项目版本加 1。 |

## Skill

### Skill 列表与版本

| 方法 | 路径 | 返回 |
| --- | --- | --- |
| GET | `/skills` | 启用的 `Skill[]` |
| GET | `/skills/{id}/versions` | 该 Skill 的 `SkillVersion[]`，按创建时间倒序 |

### 创建 Skill 版本

`POST /skills/{id}/versions`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| versionNo | string | 是 | 1–40 字符，同一 Skill 下唯一 |
| systemPromptTemplate | string | 是 | 系统提示词，1–30,000 字符 |
| inputSchema / outputSchema | object | 否 | 输入/输出 Schema |
| splitRules / rewriteRules | object | 否 | 分段与改写规则 |
| modelParameters | object | 否 | 模型参数 |

新版本状态为 `DRAFT`。重复版本号返回 `SKILL_VERSION_EXISTS`（409）。

### 测试与发布版本

| 方法 | 路径 | 请求体 | 返回 |
| --- | --- | --- | --- |
| POST | `/skill-versions/{id}/test` | 无 | `{ "status":"PASSED", "versionId": 1, "checks":[...] }` |
| POST | `/skill-versions/{id}/publish` | 无 | 发布后的 `SkillVersion` |

仅 `DRAFT` 或 `TESTING` 可发布；AI 分析只能使用 `PUBLISHED` 版本。

## AI 台词分析

### 创建分析

`POST /projects/{projectId}/analysis-runs`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| skillVersionId | number | 是 | 必须是已发布的 Skill 版本 |
| rewriteMode | string | 否 | 改写模式；默认 `STRICT` |
| providerId | number | 否 | LLM Provider ID；与 `modelId` 必须同时传或同时不传 |
| modelId | number | 否 | LLM 模型 ID；不传二者时使用本地规则 Provider |
| localRules | object | 否 | 仅本地规则可用；覆盖 Skill 版本的分段规则。支持 `maxChars`（1–500）、`punctuation`、`splitOnNewline`、`minChars`。实际规则会随分析运行快照保存。 |

接口在当前实现中同步执行分析，但仍建议以返回的 `status` 判断结果。正常完成为 `SUCCEEDED`，失败记录为 `FAILED`；Provider/模型不匹配返回 `PROVIDER_INVALID_MODEL`。`JIAN_YING_TTS` 项目只能省略 Provider/模型并使用本地规则；`EXTERNAL_AUDIO` 项目必须同时指定二者。

### 查询与应用分析

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| GET | `/projects/{projectId}/analysis-runs/{runId}` | 无 | 返回 `AnalysisRunView`，包含候选结果 `candidateResultJson`。原始供应商响应和完整输入快照只保留在服务端。 |
| POST | `/projects/{projectId}/analysis-runs/{runId}/apply` | `{ "projectVersion": 1 }` | 将分析结果替换为正式分段，返回 `DialogueSegment[]` |

### 项目历史

以下三个接口用于在重新进入项目后恢复任务、导出和分析状态。它们都按 `createdAt`、`id` 倒序返回，默认 `limit=20`，最大为 100。将响应中的 `nextCursor` 传回 `cursor` 可继续翻页。

| 方法 | 路径 | 返回 |
| --- | --- | --- |
| GET | `/projects/{projectId}/analysis-runs?cursor=&limit=20` | `{ items: AnalysisRunView[], nextCursor, hasMore }` |
| GET | `/projects/{projectId}/audio-tasks?cursor=&limit=20` | `{ items: GenerationTask[], nextCursor, hasMore }` |
| GET | `/projects/{projectId}/exports?cursor=&limit=20` | `{ items: ExportRecord[], nextCursor, hasMore }` |

仅 `SUCCEEDED` 的运行可应用。应用要求项目版本与运行时快照一致，且项目不存在人工编辑的分段；否则分别返回 `PROJECT_VERSION_CONFLICT` 或 `DIALOGUE_MANUAL_EDIT_CONFLICT`（409）。成功后运行状态为 `APPLIED`。

## 台词分段

### 查询列表

`GET /projects/{projectId}/segments`

返回当前有效的 `DialogueSegment[]`，按 `segmentNo` 升序。

### 更新单个分段

`PUT /segments/{id}`

所有字段除 `version` 外均为可选；未传字段保持原值。文本长度：`spokenText`、`subtitleText` ≤ 5,000，`voiceDirection` ≤ 2,000，`rewriteReason` ≤ 1,000；`pauseBeforeMs` 为 0–3,000，`pauseAfterMs` 为 0–10,000。

```json
{
  "version": 1,
  "spokenText": "这是一句用于配音的文本。",
  "subtitleText": "这是一句字幕。",
  "speakerName": "小王",
  "semanticGroup": "scene-1",
  "emotion": {"primary":"calm","intensity":0.7},
  "tone": ["natural"],
  "pauseBeforeMs": 100,
  "pauseAfterMs": 250,
  "speed": 1.0,
  "volume": 1.0,
  "emphasis": ["一句"],
  "voiceDirection": "自然、沉稳",
  "rewriteMode": "STRICT",
  "rewriteLevel": "MANUAL",
  "rewriteReason": "人工润色"
}
```

返回更新后的 `DialogueSegment`。修改 `spokenText`、`voiceDirection` 或 `speed` 会使 `audioStale` 置为 `1`，既有音频资产状态变为 `STALE`。

### 拆分、合并与排序

| 方法 | 路径 | 请求体 | 返回/限制 |
| --- | --- | --- | --- |
| POST | `/segments/{id}/split` | `{ "version": 1, "splitOffset": 8 }` | 返回该项目完整分段列表；`splitOffset` 为原始文本字符偏移，必须在文本内部 |
| POST | `/segments/merge` | `{ "firstSegmentId": 10, "secondSegmentId": 11, "firstVersion": 1, "secondVersion": 1 }` | 仅可合并同项目相邻分段，返回完整列表 |
| POST | `/projects/{projectId}/segments/reorder` | `{ "projectVersion": 1, "segmentIds": [11,10,12] }` | 列表必须包含该项目全部有效分段且无重复，返回排序后完整列表 |

拆分、合并、排序均可能返回 `DIALOGUE_VERSION_CONFLICT` 或 `PROJECT_VERSION_CONFLICT`（409）。

## 音频生成

### 查询可用 Provider 与模型

`GET /audio/providers`

返回：`{ "providers": Provider[], "models": Model[] }`。接口返回所有启用模型，前端应根据 `model.providerId === provider.id` 进行关联，并针对音频生成只选择 `providerType/modelType` 为 `AUDIO` 的组合。

### 创建音频任务

`POST /projects/{projectId}/audio-tasks`

可选请求头：`Idempotency-Key: <客户端生成的唯一键>`。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| providerId | number | 是 | 启用的音频 Provider ID |
| modelId | number | 是 | 且必须属于该 Provider 的启用模型 |
| segmentIds | number[] | 是 | 至少一个，且均属于该项目 |
| voiceId | string | 否 | 可选音色 ID；不传且没有参考音频时，由 SeedAudio 根据 `text_prompt` 自动生成声音。 |
| referenceAudioAssetIds | number[] | 否 | 通过参考音频上传接口获得的资产 ID，最多 3 个；与 `voiceId`、`referenceAudioData`、`referenceAudioUrl`、图片参考参数互斥。 |
| parameters | object | 否 | Provider 参数；常用 `format`、`instruction`、`speed`、`volume`、`emotion` |

```json
{"providerId":2,"modelId":3,"segmentIds":[101,102],"referenceAudioAssetIds":[301],"parameters":{"format":"wav","speed":1.0,"volume":1.0,"instruction":"自然、清晰"}}
```

立即返回 `GenerationTask`（初始 `PENDING`），后台异步执行。分段自己的 `voiceDirection`、`speed`、`volume`、`emotionJson` 会在同名参数未传时自动补入子任务。

### 上传参考音频

`POST /projects/{projectId}/audio-reference-assets`，使用 `multipart/form-data`，字段名为 `file`。支持 wav、mp3、pcm、ogg，最大 10 MB；返回 `AudioAsset`。将返回的 `id` 放进创建任务请求的 `referenceAudioAssetIds`。参考音频资产只保存一份，生成时才转换为 SeedAudio 所需的 Base64 数据，不会重复写入每个子任务。

### 状态推送、取消与重试

| 方法 | 路径 | 返回/说明 |
| --- | --- | --- |
| GET | `/audio-tasks/{id}` | `{ "task": GenerationTask, "items": GenerationItem[] }` |
| GET | `/audio-tasks/{id}/events` | SSE 状态推送；事件为 `task` 或终态 `complete`，数据结构与任务详情相同。接入方法见根目录 `AUDIO_TASK_SSE_FRONTEND.md`。 |
| POST | `/audio-tasks/{id}/cancel` | 请求取消；终态任务直接原样返回 |
| POST | `/audio-items/{id}/retry` | 仅 `FAILED` 子任务可重试，返回重新置为 `WAITING` 的 `GenerationItem` |

### 音频资产试听与下载

`GenerationItem.audioAssetId` 指向成功生成的音频资产。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/audio-assets/{id}` | 返回 `AudioAsset` 元数据。 |
| GET | `/audio-assets/{id}/download` | 直接返回音频二进制，带媒体类型，可用作 `<audio>` 的 `src`。 |

当分段包含 `emotion: { primary, secondary, intensity }` 时，服务端会将其转换为 SeedAudio 的中文 `text_prompt` 情绪要求，并和 `voiceDirection`、台词一并发送；`intensity` 会规范化到 0–1。

任务状态：`PENDING`、`RUNNING`、`CANCEL_REQUESTED`、`SUCCEEDED`、`PARTIAL_SUCCESS`、`FAILED`、`CANCELLED`。子任务状态：`WAITING`、`GENERATING`、`SUCCESS`、`FAILED`。前端订阅 SSE 至终态；任务与子任务的 `errorCode`、`errorMessage` 用于展示失败原因。

## 基础数据管理

管理接口与运行时的 `/audio/providers` 不同：本节接口会返回已停用但未删除的数据，供管理后台维护。所有更新和删除均需当前 `version`；删除为软删除。

### Provider 管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/providers` | 查询全部未删除 Provider |
| POST | `/providers` | 新增 Provider |
| PUT | `/providers/{id}` | 更新 Provider（`providerCode` 创建后不可修改） |
| DELETE | `/providers/{id}?version=1` | 软删除 Provider |

新增请求：

```json
{
  "providerCode": "my-audio-provider",
  "displayName": "我的语音服务",
  "providerType": "AUDIO",
  "baseUrl": "https://api.example.com",
  "credentialRef": "MY_AUDIO_API_KEY",
  "maskedCredentialHint": "sk-****1234",
  "timeoutSeconds": 120,
  "maxConcurrency": 4,
  "config": { "region": "cn" },
  "enabled": true
}
```

`providerCode` 全局唯一。删除前必须先删除或迁移其所有未删除模型，否则返回 `PROVIDER_HAS_MODELS`。

### 模型管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/models` | 查询全部未删除模型；可加 `?providerId=1` 筛选 |
| POST | `/models` | 新增模型 |
| PUT | `/models/{id}` | 更新模型（`modelCode` 与所属 Provider 创建后不可修改） |
| DELETE | `/models/{id}?version=1` | 软删除模型 |

```json
{
  "providerId": 1,
  "modelCode": "tts-v1",
  "displayName": "TTS V1",
  "modelType": "AUDIO",
  "capabilities": { "formats": ["wav", "mp3"], "emotion": true },
  "defaultParameters": { "format": "wav" },
  "enabled": true,
  "sortNo": 10
}
```

同一 Provider 下的 `modelCode` 唯一。`providerType`/`modelType` 建议使用 `AUDIO` 或 `LLM`；业务执行时会据此校验类型。

### 项目角色管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/projects/{projectId}/characters` | 查询项目角色，按 `sortNo`、名称排序 |
| POST | `/projects/{projectId}/characters` | 新增项目角色 |
| PUT | `/characters/{id}` | 更新角色；`characterCode` 创建后不可修改 |
| DELETE | `/characters/{id}?version=1` | 软删除角色 |

```json
{
  "characterCode": "XIAO_WANG",
  "name": "小王",
  "description": "年轻的调查记者",
  "defaultProviderId": 1,
  "defaultModelId": 2,
  "defaultVoiceId": "zh_female_xiaomei",
  "voiceConfig": { "speed": 1.0, "volume": 1.0 },
  "sortNo": 10
}
```

角色默认 Provider 与模型必须同时提供，且模型必须属于该 Provider；二者必须均为已启用的 `AUDIO` 类型。项目内 `characterCode` 唯一。

## 导出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects/{projectId}/exports/srt` | 创建 SRT 导出；可选 `Idempotency-Key` |
| POST | `/projects/{projectId}/exports/package` | 创建 ZIP 素材包；可选 `Idempotency-Key` |
| GET | `/exports/{id}` | 返回 `ExportRecord` |
| GET | `/exports/{id}/download` | 直接返回二进制文件，带 `Content-Disposition: attachment`，不是 JSON 信封 |

当前实现中导出在请求内完成，但前端仍应检查记录 `status`。`SUCCEEDED` 时才可下载；未就绪返回 `EXPORT_NOT_READY`（400）。无有效分段时返回 `EXPORT_NO_SEGMENTS`。素材包当前包含原始文本、SRT 与 JSON 元数据。

## 响应对象速查

### Project / ProjectView

`id, projectNo, name, storyBackground, originalDialogue, outputMode, defaultSkillVersionId, status, analysisStale, version, createdAt, updatedAt`；`ProjectView` 额外有 `segments: DialogueSegment[]`，并且 `analysisStale` 是布尔值（普通 `Project` 为 `0/1` 整数）。

### Skill / SkillVersion

- `Skill`：`id, skillCode, name, description, taskType, enabled, version, createdAt, updatedAt`
- `SkillVersion`：`id, skillId, versionNo, status, systemPromptTemplate, inputSchemaJson, outputSchemaJson, splitRulesJson, rewriteRulesJson, modelParametersJson, contentHash, publishedAt, version, createdAt, updatedAt`

### AnalysisRunView

`id, runNo, projectId, projectVersion, skillVersionId, providerId, modelId, providerCodeSnapshot, modelCodeSnapshot, rewriteMode, status, candidateResultJson, validationWarningsJson, errorCode, errorMessage, startedAt, finishedAt, appliedAt, version, createdAt, updatedAt`。原始供应商响应、完整输入快照与模型参数仅留在服务端的 `AdsAnalysisRun` 审计记录中。

候选结果结构为：`{ "segments": [{ "segmentNo": 1, "speaker": "", "sourceStart": 0, "sourceEnd": 10, "originalText": "", "spokenText": "", "subtitleText": "", "semanticGroup": "", "emotion": {"primary":"", "secondary":"", "intensity":0.5}, "tone": [], "speed": 1.0, "volume": 1.0, "pauseBeforeMs": 0, "pauseAfterMs": 0, "emphasis": [], "voiceDirection": "", "rewriteMode": "", "rewriteLevel": "", "rewriteReason": "" }] }`。

### DialogueSegment

`id, projectId, segmentNo, speakerId, speakerNameSnapshot, semanticGroup, sourceStart, sourceEnd, originalText, spokenText, subtitleText, emotionJson, toneJson, speed, volume, pauseBeforeMs, pauseAfterMs, emphasisJson, voiceDirection, rewriteMode, rewriteLevel, rewriteReason, manualEdited, audioStale, analysisRunId, version, createdAt, updatedAt`。

### Provider / Model

- `Provider`：`id, providerCode, displayName, providerType, baseUrl, credentialRef, maskedCredentialHint, timeoutSeconds, maxConcurrency, configJson, enabled, version, createdAt, updatedAt`
- `Model`：`id, providerId, modelCode, displayName, modelType, capabilitiesJson, defaultParametersJson, enabled, sortNo, version, createdAt, updatedAt`

### GenerationTask / GenerationItem

- `GenerationTask`：`id, taskNo, projectId, providerId, modelId, providerCodeSnapshot, modelCodeSnapshot, idempotencyKey, parametersJson, status, totalCount, successCount, failedCount, cancelledCount, progressPercent, cancelRequestedAt, startedAt, finishedAt, errorCode, errorMessage, version, createdAt, updatedAt`
- `GenerationItem`：`id, taskId, itemNo, segmentId, segmentVersion, providerId, modelId, voiceId, textSnapshot, parametersJson, status, attemptCount, maxAttempts, externalTaskId, audioAssetId, warningsJson, errorCode, errorMessage, startedAt, finishedAt, version, createdAt, updatedAt`

### ExportRecord

`id, exportNo, projectId, exportType`（`SRT`/`PACKAGE`）、`status`（`RUNNING`/`SUCCEEDED`/`FAILED`）、`idempotencyKey, projectVersion, snapshotJson, storageProvider, objectKey, fileName, fileSizeBytes, sha256, errorCode, errorMessage, startedAt, finishedAt, expiresAt, version, createdAt, updatedAt`。

## 常见业务错误码

| 错误码 | 含义 |
| --- | --- |
| COMMON_VALIDATION_ERROR | Bean 校验失败（字段缺失、长度或范围不合法） |
| PROJECT_NOT_FOUND / PROJECT_VERSION_CONFLICT | 项目不存在 / 项目版本冲突 |
| SKILL_NOT_FOUND / SKILL_VERSION_NOT_FOUND / SKILL_VERSION_NOT_PUBLISHED | Skill 或版本不存在 / 版本未发布 |
| SKILL_RUN_NOT_FOUND / SKILL_RUN_NOT_READY | 分析运行不存在 / 尚不可应用 |
| DIALOGUE_NOT_FOUND / DIALOGUE_VERSION_CONFLICT | 分段不存在 / 分段版本冲突 |
| DIALOGUE_INVALID_SPLIT / DIALOGUE_NOT_CONTIGUOUS / DIALOGUE_INVALID_REORDER | 拆分、合并或排序条件不合法 |
| PROVIDER_INVALID_MODEL | Provider、模型不可用或不匹配 |
| AUDIO_INVALID_SEGMENTS / AUDIO_ITEM_NOT_RETRYABLE | 分段不属于项目 / 子任务不可重试 |
| EXPORT_NO_SEGMENTS / EXPORT_NOT_READY / EXPORT_FILE_MISSING | 无可导出分段 / 导出未完成 / 导出文件不存在 |
| PROVIDER_CODE_EXISTS / PROVIDER_HAS_MODELS / PROVIDER_VERSION_CONFLICT | Provider 编码重复 / 仍关联模型 / 版本冲突 |
| MODEL_CODE_EXISTS / MODEL_VERSION_CONFLICT | 模型编码重复 / 版本冲突 |
| CHARACTER_CODE_EXISTS / CHARACTER_INVALID_VOICE_BINDING / CHARACTER_VERSION_CONFLICT | 项目角色编码重复 / 默认音频绑定不合法 / 版本冲突 |
