package com.ckrey.autobackworkflow.audio.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.audio.application.AudioTaskApplicationService;
import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsGenerationItem;
import com.ckrey.autobackworkflow.domain.AdsGenerationTask;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.domain.AdsProvider;
import com.ckrey.autobackworkflow.service.AdsModelService;
import com.ckrey.autobackworkflow.service.AdsProviderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class AudioController {
    private final AudioTaskApplicationService audio; private final AdsProviderService providers; private final AdsModelService models;
    public AudioController(AudioTaskApplicationService audio,AdsProviderService providers,AdsModelService models){this.audio=audio;this.providers=providers;this.models=models;}
    @GetMapping("/audio/providers") public ApiResponse<Map<String,Object>> providerList(){List<AdsProvider> p=providers.list(Wrappers.<AdsProvider>lambdaQuery().eq(AdsProvider::getEnabled,1).eq(AdsProvider::getDeleted,0));List<AdsModel> m=models.list(Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getEnabled,1).eq(AdsModel::getDeleted,0));return ApiResponse.ok(Map.of("providers",p,"models",m));}
    @PostMapping("/projects/{projectId}/audio-tasks") public ApiResponse<AdsGenerationTask> create(@PathVariable Long projectId,@Valid @RequestBody AudioDtos.CreateTaskRequest request,@RequestHeader(value="Idempotency-Key",required=false)String key){return ApiResponse.ok(audio.create(projectId,request,key));}
    @GetMapping("/audio-tasks/{id}") public ApiResponse<Map<String,Object>> get(@PathVariable Long id){return ApiResponse.ok(Map.of("task",audio.get(id),"items",audio.itemList(id)));}
    @PostMapping("/audio-tasks/{id}/cancel") public ApiResponse<AdsGenerationTask> cancel(@PathVariable Long id){return ApiResponse.ok(audio.cancel(id));}
    @PostMapping("/audio-items/{id}/retry") public ApiResponse<AdsGenerationItem> retry(@PathVariable Long id){return ApiResponse.ok(audio.retry(id));}
}
