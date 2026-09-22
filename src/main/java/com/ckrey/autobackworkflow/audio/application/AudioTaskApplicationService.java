package com.ckrey.autobackworkflow.audio.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.audio.api.AudioDtos;
import com.ckrey.autobackworkflow.audio.provider.AudioGenerationProvider;
import com.ckrey.autobackworkflow.audio.provider.AudioProviderRegistry;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.api.CursorPage;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.*;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import com.ckrey.autobackworkflow.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AudioTaskApplicationService {
    private final ProjectApplicationService projects;
    private final AdsProviderService providers;
    private final AdsModelService models;
    private final AdsDialogueSegmentService segments;
    private final AdsGenerationTaskService tasks;
    private final AdsGenerationItemService items;
    private final AdsAudioAssetService assets;
    private final AudioProviderRegistry registry;
    private final Executor applicationTaskExecutor;
    private final ObjectMapper mapper;
    private final Path root;

    public AudioTaskApplicationService(ProjectApplicationService projects, AdsProviderService providers, AdsModelService models, AdsDialogueSegmentService segments, AdsGenerationTaskService tasks, AdsGenerationItemService items, AdsAudioAssetService assets, AudioProviderRegistry registry, Executor applicationTaskExecutor, ObjectMapper mapper, @Value("${ads.storage.root:./data/assets}") String root) {
        this.projects = projects;
        this.providers = providers;
        this.models = models;
        this.segments = segments;
        this.tasks = tasks;
        this.items = items;
        this.assets = assets;
        this.registry = registry;
        this.applicationTaskExecutor = applicationTaskExecutor;
        this.mapper = mapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional
    public AdsGenerationTask create(Long projectId, AudioDtos.CreateTaskRequest request, String key) {
        projects.required(projectId);
        AdsProvider provider = providers.getById(request.providerId());
        AdsModel model = models.getById(request.modelId());
        if (provider == null || provider.getEnabled() != 1 || model == null || model.getEnabled() != 1 || !provider.getId().equals(model.getProviderId()))
            throw BizException.badRequest("PROVIDER_INVALID_MODEL", "Provider 或模型不可用");
        validateReferenceMode(projectId, request, provider);
        List<AdsDialogueSegment> selected = segments.list(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getProjectId, projectId).eq(AdsDialogueSegment::getDeleted, 0).in(AdsDialogueSegment::getId, request.segmentIds()).orderByAsc(AdsDialogueSegment::getSegmentNo));
        if (selected.size() != new HashSet<>(request.segmentIds()).size())
            throw BizException.badRequest("AUDIO_INVALID_SEGMENTS", "存在不属于项目的分段");
        if (key != null && !key.isBlank()) {
            AdsGenerationTask existing = tasks.getOne(Wrappers.<AdsGenerationTask>lambdaQuery().eq(AdsGenerationTask::getProjectId, projectId).eq(AdsGenerationTask::getIdempotencyKey, key));
            if (existing != null) return existing;
        }
        Date now = new Date();
        AdsGenerationTask task = new AdsGenerationTask();
        task.setTaskNo("AT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        task.setProjectId(projectId);
        task.setProviderId(provider.getId());
        task.setModelId(model.getId());
        task.setProviderCodeSnapshot(provider.getProviderCode());
        task.setModelCodeSnapshot(model.getModelCode());
        task.setIdempotencyKey(key);
        Map<String, Object> taskParameters = new LinkedHashMap<>(request.parameters() == null ? Map.of() : request.parameters());
        if (request.referenceAudioAssetIds() != null && !request.referenceAudioAssetIds().isEmpty()) {
            taskParameters.put("referenceAudioAssetIds", request.referenceAudioAssetIds());
        }
        task.setParametersJson(Hashing.json(taskParameters));
        task.setRequestHash(Hashing.sha256(projectId + ":" + request.modelId() + ":" + Hashing.json(request)));
        task.setStatus("PENDING");
        task.setTotalCount(selected.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setCancelledCount(0);
        task.setProgressPercent(BigDecimal.ZERO);
        task.setVersion(1);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        tasks.save(task);
        int n = 1;
        for (AdsDialogueSegment s : selected) {
            Map<String, Object> itemParameters = new LinkedHashMap<>(taskParameters);
            if (s.getVoiceDirection() != null) itemParameters.putIfAbsent("instruction", s.getVoiceDirection());
            if (s.getSpeed() != null) itemParameters.putIfAbsent("speed", s.getSpeed());
            if (s.getVolume() != null) itemParameters.putIfAbsent("volume", s.getVolume());
            if (s.getEmotionJson() != null) itemParameters.putIfAbsent("emotion", s.getEmotionJson());
            AdsGenerationItem i = new AdsGenerationItem();
            i.setTaskId(task.getId());
            i.setItemNo(n++);
            i.setSegmentId(s.getId());
            i.setSegmentVersion(s.getVersion());
            i.setProviderId(provider.getId());
            i.setModelId(model.getId());
            i.setProviderCodeSnapshot(provider.getProviderCode());
            i.setModelCodeSnapshot(model.getModelCode());
            i.setVoiceId(request.voiceId() == null ? "" : request.voiceId().trim());
            i.setTextSnapshot(s.getSpokenText());
            i.setParametersJson(Hashing.json(itemParameters));
            i.setRequestHash(Hashing.sha256(projectId + ":" + s.getId() + ":" + s.getVersion() + ":" + model.getModelCode() + ":" + i.getVoiceId() + ":" + Hashing.json(itemParameters)));
            i.setStatus("WAITING");
            i.setAttemptCount(0);
            i.setMaxAttempts(3);
            i.setVersion(1);
            i.setCreatedAt(now);
            i.setUpdatedAt(now);
            items.save(i);
        }
        Long taskId = task.getId();
        applicationTaskExecutor.execute(() -> run(taskId));
        return task;
    }

    public AdsGenerationTask get(Long id) {
        AdsGenerationTask t = tasks.getById(id);
        if (t == null) throw BizException.notFound("AUDIO_TASK_NOT_FOUND", "音频任务不存在");
        return t;
    }

    public CursorPage<AdsGenerationTask> list(Long projectId, String cursor, Integer requestedLimit) {
        projects.required(projectId);
        int limit = CursorPage.limit(requestedLimit);
        CursorPage.Cursor anchor = CursorPage.decode(cursor);
        var query = Wrappers.<AdsGenerationTask>lambdaQuery().eq(AdsGenerationTask::getProjectId, projectId);
        if (anchor != null) {
            query.and(group -> group.lt(AdsGenerationTask::getCreatedAt, anchor.createdAt())
                    .or(tie -> tie.eq(AdsGenerationTask::getCreatedAt, anchor.createdAt())
                            .lt(AdsGenerationTask::getId, anchor.id())));
        }
        List<AdsGenerationTask> rows = tasks.list(query.orderByDesc(AdsGenerationTask::getCreatedAt)
                .orderByDesc(AdsGenerationTask::getId).last("LIMIT " + (limit + 1)));
        return CursorPage.from(rows, limit, AdsGenerationTask::getCreatedAt, AdsGenerationTask::getId);
    }

    public List<AdsGenerationItem> itemList(Long id) {
        get(id);
        return items.list(Wrappers.<AdsGenerationItem>lambdaQuery().eq(AdsGenerationItem::getTaskId, id).orderByAsc(AdsGenerationItem::getItemNo));
    }

    public AdsAudioAsset asset(Long id) {
        AdsAudioAsset asset = assets.getOne(Wrappers.<AdsAudioAsset>lambdaQuery().eq(AdsAudioAsset::getId, id)
                .eq(AdsAudioAsset::getDeleted, 0));
        if (asset == null) throw BizException.notFound("AUDIO_ASSET_NOT_FOUND", "音频资产不存在");
        return asset;
    }

    public Resource downloadAsset(Long id) {
        AdsAudioAsset asset = asset(id);
        Path path = root.resolve(asset.getObjectKey()).normalize();
        if (!path.startsWith(root)) throw BizException.badRequest("AUDIO_ASSET_INVALID_PATH", "音频资产路径不合法");
        if (!Files.isRegularFile(path)) throw BizException.notFound("AUDIO_ASSET_FILE_MISSING", "音频资产文件不存在或已过期");
        return new FileSystemResource(path);
    }

    @Transactional
    public AdsAudioAsset uploadReferenceAudio(Long projectId, MultipartFile file) {
        projects.required(projectId);
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "请上传非空的参考音频");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频不能超过 10 MB");
        }
        String extension = referenceExtension(file.getOriginalFilename(), file.getContentType());
        Date now = new Date();
        AdsAudioAsset asset = new AdsAudioAsset();
        asset.setAssetNo("RF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        asset.setProjectId(projectId);
        asset.setAssetType("REFERENCE");
        asset.setStatus("ACTIVE");
        asset.setStorageProvider("LOCAL");
        asset.setOriginalFileName(safeFileName(file.getOriginalFilename(), "reference." + extension));
        asset.setMediaType(referenceMediaType(extension));
        asset.setObjectKey("references/" + asset.getAssetNo() + "/" + asset.getOriginalFileName());
        asset.setVersion(1);
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        asset.setDeleted(0);
        Path target = root.resolve(asset.getObjectKey()).normalize();
        if (!target.startsWith(root)) throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频路径不合法");
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            asset.setFileSizeBytes(Files.size(target));
            asset.setSha256(sha(target));
            assets.save(asset);
            return asset;
        } catch (Exception ex) {
            throw BizException.badRequest("AUDIO_REFERENCE_UPLOAD_FAILED", "参考音频上传失败");
        }
    }

    @Transactional
    public AdsGenerationTask cancel(Long id) {
        AdsGenerationTask t = get(id);
        if (Set.of("SUCCEEDED", "FAILED", "PARTIAL_SUCCESS", "CANCELLED").contains(t.getStatus())) return t;
        t.setStatus("CANCEL_REQUESTED");
        t.setCancelRequestedAt(new Date());
        tasks.updateById(t);
        return get(id);
    }

    @Transactional
    public AdsGenerationItem retry(Long itemId) {
        AdsGenerationItem item = items.getById(itemId);
        if (item == null) throw BizException.notFound("AUDIO_ITEM_NOT_FOUND", "音频子任务不存在");
        if (!"FAILED".equals(item.getStatus()))
            throw BizException.badRequest("AUDIO_ITEM_NOT_RETRYABLE", "仅失败的子任务可以重试");
        item.setStatus("WAITING");
        item.setErrorCode(null);
        item.setErrorMessage(null);
        items.updateById(item);
        applicationTaskExecutor.execute(() -> run(item.getTaskId()));
        return item;
    }

    private void run(Long id) {
        AdsGenerationTask task = get(id);
        if ("CANCEL_REQUESTED".equals(task.getStatus())) {
            finishCancelled(task);
            return;
        }
        task.setStatus("RUNNING");
        task.setStartedAt(task.getStartedAt() == null ? new Date() : task.getStartedAt());
        tasks.updateById(task);
        for (AdsGenerationItem item : itemList(id)) {
            if (!"WAITING".equals(item.getStatus())) continue;
            if ("CANCEL_REQUESTED".equals(get(id).getStatus())) break;
            generate(task, item);
        }
        finish(id);
    }

    private void generate(AdsGenerationTask task, AdsGenerationItem item) {
        item.setStatus("GENERATING");
        item.setAttemptCount(item.getAttemptCount() + 1);
        item.setStartedAt(new Date());
        items.updateById(item);
        try {
            AudioGenerationProvider provider = registry.getRequired(task.getProviderCodeSnapshot());
            Map<String, Object> parameters = resolveReferenceAudio(task.getProjectId(), readParameters(item.getParametersJson()));
            String format = stringParameter(parameters, "format", "wav");
            AudioGenerationProvider.VoiceDirection direction = new AudioGenerationProvider.VoiceDirection(
                    stringParameter(parameters, "instruction", null), doubleParameter(parameters, "speed", 1d),
                    doubleParameter(parameters, "volume", 1d), AudioTaskParameters.emotion(parameters.get("emotion"), mapper));
            AudioGenerationProvider.AudioGenerationResult result = provider.generate(new AudioGenerationProvider.AudioGenerationCommand(
                    item.getRequestHash(), task.getModelCodeSnapshot(), item.getTextSnapshot(), item.getVoiceId(), direction, format, parameters));
            String extension = extension(result.mediaType(), format);
            Path destination = root.resolve("audio/" + task.getTaskNo() + "/" + String.format("%03d.%s", item.getItemNo(), extension)).normalize();
            if (!destination.startsWith(root)) throw new IllegalStateException("invalid asset path");
            Files.createDirectories(destination.getParent());
            Files.write(destination, result.audio());
            AdsAudioAsset asset = new AdsAudioAsset();
            asset.setAssetNo("AS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            asset.setProjectId(task.getProjectId());
            asset.setSegmentId(item.getSegmentId());
            asset.setSegmentVersion(item.getSegmentVersion());
            asset.setGenerationTaskId(task.getId());
            asset.setAssetType("SEGMENT");
            asset.setStatus("ACTIVE");
            asset.setStorageProvider("LOCAL");
            asset.setObjectKey(root.relativize(destination).toString());
            asset.setOriginalFileName(destination.getFileName().toString());
            asset.setMediaType(result.mediaType());
            asset.setFileSizeBytes(Files.size(destination));
            asset.setSha256(sha(destination));
            asset.setDurationMs(longMetadata(result.metadata(), "durationMs", estimatedPcmDuration(result.audio(), format, result.metadata())));
            asset.setSampleRateHz(intMetadata(result.metadata(), "sampleRate", null));
            asset.setBitDepth(Set.of("wav", "pcm").contains(format) ? 16 : null);
            asset.setChannels(Set.of("wav", "pcm").contains(format) ? 1 : null);
            asset.setManifestJson(Hashing.json(Map.of("warnings", result.warnings(), "providerMetadata", result.metadata())));
            asset.setVersion(1);
            asset.setCreatedAt(new Date());
            asset.setUpdatedAt(new Date());
            asset.setDeleted(0);
            assets.save(asset);
            item.setAudioAssetId(asset.getId());
            item.setExternalTaskId(result.externalTaskId());
            item.setWarningsJson(Hashing.json(result.warnings()));
            item.setStatus("SUCCESS");
            item.setFinishedAt(new Date());
            items.updateById(item);
        } catch (Exception ex) {
            item.setStatus("FAILED");
            item.setErrorCode(ex instanceof BizException b ? b.getCode() : "AUDIO_GENERATION_FAILED");
            item.setErrorMessage("音频生成失败");
            item.setFinishedAt(new Date());
            items.updateById(item);
        }
    }

    private void finish(Long id) {
        AdsGenerationTask task = get(id);
        List<AdsGenerationItem> list = itemList(id);
        int success = (int) list.stream().filter(i -> "SUCCESS".equals(i.getStatus())).count();
        int failed = (int) list.stream().filter(i -> "FAILED".equals(i.getStatus())).count();
        boolean cancelling = "CANCEL_REQUESTED".equals(task.getStatus());
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setCancelledCount(cancelling ? task.getTotalCount() - success - failed : 0);
        task.setProgressPercent(new BigDecimal("100"));
        task.setStatus(cancelling ? "CANCELLED" : failed == 0 ? "SUCCEEDED" : success > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        task.setFinishedAt(new Date());
        tasks.updateById(task);
    }

    private void finishCancelled(AdsGenerationTask task) {
        task.setStatus("CANCELLED");
        task.setFinishedAt(new Date());
        tasks.updateById(task);
    }

    private static String sha(Path p) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p));
        StringBuilder s = new StringBuilder();
        for (byte b : d) s.append(String.format("%02x", b));
        return s.toString();
    }

    private Map<String, Object> readParameters(Object value) {
        if (value == null) return new LinkedHashMap<>();
        try {
            if (value instanceof String text) return mapper.readValue(text, new TypeReference<>() { });
            return mapper.convertValue(value, new TypeReference<>() { });
        } catch (Exception ex) {
            throw BizException.badRequest("AUDIO_INVALID_PARAMETERS", "保存的音频参数无法读取");
        }
    }
    private void validateReferenceMode(Long projectId, AudioDtos.CreateTaskRequest request, AdsProvider provider) {
        List<Long> referenceIds = request.referenceAudioAssetIds() == null ? List.of() : request.referenceAudioAssetIds();
        if (referenceIds.size() != new HashSet<>(referenceIds).size()) {
            throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频不能重复");
        }
        Map<String, Object> parameters = request.parameters() == null ? Map.of() : request.parameters();
        boolean directReference = parameters.containsKey("referenceAudioData") || parameters.containsKey("referenceAudioUrl")
                || parameters.containsKey("referenceImageData") || parameters.containsKey("referenceImageUrl");
        boolean voiceSelected = request.voiceId() != null && !request.voiceId().isBlank();
        if (voiceSelected && (!referenceIds.isEmpty() || directReference)) {
            throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "音色 ID 不能与参考音频或参考图片同时使用");
        }
        if (!referenceIds.isEmpty()) {
            if (!"seed-audio".equals(provider.getProviderCode())) {
                throw BizException.badRequest("AUDIO_REFERENCE_UNSUPPORTED", "当前 Provider 不支持上传参考音频");
            }
            if (directReference) {
                throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "上传参考音频后不能再传入其他参考素材参数");
            }
            List<AdsAudioAsset> assetsForProject = assets.list(Wrappers.<AdsAudioAsset>lambdaQuery()
                    .eq(AdsAudioAsset::getProjectId, projectId).eq(AdsAudioAsset::getAssetType, "REFERENCE")
                    .eq(AdsAudioAsset::getStatus, "ACTIVE").eq(AdsAudioAsset::getDeleted, 0)
                    .in(AdsAudioAsset::getId, referenceIds));
            if (assetsForProject.size() != referenceIds.size()) {
                throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "存在不可用或不属于项目的参考音频");
            }
        }
    }
    private Map<String, Object> resolveReferenceAudio(Long projectId, Map<String, Object> parameters) {
        Object value = parameters.get("referenceAudioAssetIds");
        if (value == null) return parameters;
        List<Long> ids;
        try { ids = mapper.convertValue(value, new TypeReference<>() { }); }
        catch (IllegalArgumentException ex) { throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频参数无法读取"); }
        if (ids == null || ids.isEmpty() || ids.size() > 3) {
            throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频数量必须在 1 到 3 之间");
        }
        List<AdsAudioAsset> rows = assets.list(Wrappers.<AdsAudioAsset>lambdaQuery()
                .eq(AdsAudioAsset::getProjectId, projectId).eq(AdsAudioAsset::getAssetType, "REFERENCE")
                .eq(AdsAudioAsset::getStatus, "ACTIVE").eq(AdsAudioAsset::getDeleted, 0)
                .in(AdsAudioAsset::getId, ids));
        if (rows.size() != ids.size()) throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频不存在或不可用");
        Map<Long, AdsAudioAsset> byId = new HashMap<>();
        rows.forEach(asset -> byId.put(asset.getId(), asset));
        List<String> audioData = new ArrayList<>();
        for (Long id : ids) {
            AdsAudioAsset asset = byId.get(id);
            Path path = root.resolve(asset.getObjectKey()).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw BizException.notFound("AUDIO_REFERENCE_FILE_MISSING", "参考音频文件不存在或已过期");
            }
            try { audioData.add(Base64.getEncoder().encodeToString(Files.readAllBytes(path))); }
            catch (Exception ex) { throw BizException.badRequest("AUDIO_REFERENCE_FILE_MISSING", "参考音频文件无法读取"); }
        }
        Map<String, Object> resolved = new LinkedHashMap<>(parameters);
        resolved.remove("referenceAudioAssetIds");
        resolved.put("referenceAudioData", audioData);
        return resolved;
    }
    private static String stringParameter(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key); return value instanceof String text && !text.isBlank() ? text : fallback;
    }
    private static Double doubleParameter(Map<String, Object> values, String key, Double fallback) {
        Object value = values.get(key); return value instanceof Number number ? number.doubleValue() : fallback;
    }
    private static String extension(String mediaType, String fallback) {
        if ("audio/mpeg".equalsIgnoreCase(mediaType)) return "mp3";
        if ("audio/ogg".equalsIgnoreCase(mediaType)) return "ogg";
        if ("audio/L16".equalsIgnoreCase(mediaType)) return "pcm";
        if ("audio/wav".equalsIgnoreCase(mediaType)) return "wav";
        return fallback.replaceAll("[^a-zA-Z0-9]", "");
    }
    private static String referenceExtension(String fileName, String mediaType) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".wav") || "audio/wav".equalsIgnoreCase(mediaType) || "audio/x-wav".equalsIgnoreCase(mediaType)) return "wav";
        if (name.endsWith(".mp3") || "audio/mpeg".equalsIgnoreCase(mediaType)) return "mp3";
        if (name.endsWith(".pcm") || "audio/l16".equalsIgnoreCase(mediaType)) return "pcm";
        if (name.endsWith(".ogg") || "audio/ogg".equalsIgnoreCase(mediaType)) return "ogg";
        throw BizException.badRequest("AUDIO_REFERENCE_INVALID", "参考音频只支持 wav、mp3、pcm 或 ogg");
    }
    private static String referenceMediaType(String extension) {
        return switch (extension) { case "wav" -> "audio/wav"; case "mp3" -> "audio/mpeg"; case "pcm" -> "audio/L16"; default -> "audio/ogg"; };
    }
    private static String safeFileName(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String sanitized = Path.of(value).getFileName().toString().replaceAll("[^\\p{IsHan}A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? fallback : sanitized.substring(0, Math.min(sanitized.length(), 200));
    }
    private static Long estimatedPcmDuration(byte[] audio, String format, Map<String, Object> metadata) {
        if (!Set.of("wav", "pcm").contains(format)) return null;
        Integer rate = intMetadata(metadata, "sampleRate", 40000);
        int header = "wav".equals(format) && audio.length >= 44 ? 44 : 0;
        return Math.round(Math.max(0, audio.length - header) / 2d / rate * 1000d);
    }
    private static Long longMetadata(Map<String, Object> metadata, String key, Long fallback) {
        Object value = metadata == null ? null : metadata.get(key); return value instanceof Number number ? number.longValue() : fallback;
    }
    private static Integer intMetadata(Map<String, Object> metadata, String key, Integer fallback) {
        Object value = metadata == null ? null : metadata.get(key); return value instanceof Number number ? number.intValue() : fallback;
    }
}
