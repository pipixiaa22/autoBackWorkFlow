package com.ckrey.autobackworkflow.catalog.api;

import com.ckrey.autobackworkflow.catalog.application.CatalogApplicationService;
import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.domain.AdsProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogApplicationService catalog;

    public CatalogController(CatalogApplicationService catalog) { this.catalog = catalog; }

    @GetMapping("/providers")
    public ApiResponse<List<AdsProvider>> providers() { return ApiResponse.ok(catalog.providers()); }

    @PostMapping("/providers")
    public ApiResponse<AdsProvider> createProvider(@Valid @RequestBody CatalogDtos.CreateProviderRequest request) {
        return ApiResponse.ok(catalog.createProvider(request));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<AdsProvider> updateProvider(@PathVariable Long id, @Valid @RequestBody CatalogDtos.UpdateProviderRequest request) {
        return ApiResponse.ok(catalog.updateProvider(id, request));
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id, @RequestParam Integer version) {
        catalog.deleteProvider(id, version); return ApiResponse.ok(null);
    }

    @GetMapping("/models")
    public ApiResponse<List<AdsModel>> models(@RequestParam(required = false) Long providerId) {
        return ApiResponse.ok(catalog.models(providerId));
    }

    @PostMapping("/models")
    public ApiResponse<AdsModel> createModel(@Valid @RequestBody CatalogDtos.CreateModelRequest request) {
        return ApiResponse.ok(catalog.createModel(request));
    }

    @PutMapping("/models/{id}")
    public ApiResponse<AdsModel> updateModel(@PathVariable Long id, @Valid @RequestBody CatalogDtos.UpdateModelRequest request) {
        return ApiResponse.ok(catalog.updateModel(id, request));
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> deleteModel(@PathVariable Long id, @RequestParam Integer version) {
        catalog.deleteModel(id, version); return ApiResponse.ok(null);
    }
}
