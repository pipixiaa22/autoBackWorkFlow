package com.ckrey.autobackworkflow.audio.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.audio.application.AudioTaskApplicationService;
import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsGenerationItem;
import com.ckrey.autobackworkflow.domain.AdsGenerationTask;
import com.ckrey.autobackworkflow.domain.AdsAudioAsset;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.domain.AdsProvider;
import com.ckrey.autobackworkflow.service.AdsModelService;
import com.ckrey.autobackworkflow.service.AdsProviderService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AudioController {
    private final AudioTaskApplicationService audio;
    private final AdsProviderService providers;
    private final AdsModelService models;

    public AudioController(AudioTaskApplicationService audio, AdsProviderService providers, AdsModelService models) {
        this.audio = audio;
        this.providers = providers;
        this.models = models;
    }

    @GetMapping("/audio/providers")
    public ApiResponse<Map<String, Object>> providerList() {
        List<AdsProvider> p = providers.list(Wrappers.<AdsProvider>lambdaQuery().eq(AdsProvider::getEnabled, 1).eq(AdsProvider::getDeleted, 0));
        List<AdsModel> m = models.list(Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getEnabled, 1).eq(AdsModel::getDeleted, 0));
        return ApiResponse.ok(Map.of("providers", p, "models", m));
    }

    @PostMapping(value = "/projects/{projectId}/audio-reference-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdsAudioAsset> uploadReferenceAudio(@PathVariable Long projectId, @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.ok(audio.uploadReferenceAudio(projectId, file));
    }

    @PostMapping("/projects/{projectId}/audio-tasks")
    public ApiResponse<AdsGenerationTask> create(@PathVariable Long projectId, @Valid @RequestBody AudioDtos.CreateTaskRequest request, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ApiResponse.ok(audio.create(projectId, request, key));
    }

    @GetMapping("/projects/{projectId}/audio-tasks")
    public ApiResponse<com.ckrey.autobackworkflow.common.api.CursorPage<AdsGenerationTask>> list(@PathVariable Long projectId, @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(audio.list(projectId, cursor, limit));
    }

    @GetMapping("/audio-tasks/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("task", audio.get(id), "items", audio.itemList(id)));
    }

    @PostMapping("/audio-tasks/{id}/cancel")
    public ApiResponse<AdsGenerationTask> cancel(@PathVariable Long id) {
        return ApiResponse.ok(audio.cancel(id));
    }

    @PostMapping("/audio-items/{id}/retry")
    public ApiResponse<AdsGenerationItem> retry(@PathVariable Long id) {
        return ApiResponse.ok(audio.retry(id));
    }

    @GetMapping("/audio-assets/{id}")
    public ApiResponse<AdsAudioAsset> asset(@PathVariable Long id) {
        return ApiResponse.ok(audio.asset(id));
    }

    @GetMapping("/audio-assets/{id}/download")
    public ResponseEntity<Resource> downloadAsset(@PathVariable Long id) {
        AdsAudioAsset asset = audio.asset(id);
        MediaType type;
        try {
            type = asset.getMediaType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(asset.getMediaType());
        } catch (IllegalArgumentException ex) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        String fileName = (asset.getOriginalFileName() == null ? "audio" : asset.getOriginalFileName()).replaceAll("[\\r\\n\\\"]", "_");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(type).body(audio.downloadAsset(id));
    }
}
