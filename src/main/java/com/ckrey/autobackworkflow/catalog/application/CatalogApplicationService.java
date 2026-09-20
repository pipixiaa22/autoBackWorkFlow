package com.ckrey.autobackworkflow.catalog.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.catalog.api.CatalogDtos;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.domain.AdsProvider;
import com.ckrey.autobackworkflow.service.AdsModelService;
import com.ckrey.autobackworkflow.service.AdsProviderService;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogApplicationService {
    private final AdsProviderService providers;
    private final AdsModelService models;

    public CatalogApplicationService(AdsProviderService providers, AdsModelService models) {
        this.providers = providers; this.models = models;
    }

    public List<AdsProvider> providers() {
        return providers.list(Wrappers.<AdsProvider>lambdaQuery().eq(AdsProvider::getDeleted, 0).orderByAsc(AdsProvider::getDisplayName));
    }

    @Transactional
    public AdsProvider createProvider(CatalogDtos.CreateProviderRequest request) {
        if (providers.count(Wrappers.<AdsProvider>lambdaQuery().eq(AdsProvider::getProviderCode, request.providerCode().trim()).eq(AdsProvider::getDeleted, 0)) > 0)
            throw BizException.conflict("PROVIDER_CODE_EXISTS", "Provider 编码已存在");
        Date now = new Date(); AdsProvider value = new AdsProvider();
        value.setProviderCode(request.providerCode().trim()); value.setDisplayName(request.displayName().trim()); value.setProviderType(request.providerType().trim());
        value.setBaseUrl(blankToNull(request.baseUrl())); value.setCredentialRef(blankToNull(request.credentialRef())); value.setMaskedCredentialHint(blankToNull(request.maskedCredentialHint()));
        value.setTimeoutSeconds(request.timeoutSeconds() == null ? 120 : request.timeoutSeconds()); value.setMaxConcurrency(request.maxConcurrency() == null ? 1 : request.maxConcurrency());
        value.setConfigJson(Hashing.json(request.config() == null ? java.util.Map.of() : request.config())); value.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
        value.setVersion(1); value.setDeleted(0); value.setCreatedAt(now); value.setUpdatedAt(now); providers.save(value); return value;
    }

    @Transactional
    public AdsProvider updateProvider(Long id, CatalogDtos.UpdateProviderRequest request) {
        AdsProvider value = requiredProvider(id); verifyVersion(value.getVersion(), request.version(), "PROVIDER_VERSION_CONFLICT");
        value.setDisplayName(request.displayName().trim()); value.setProviderType(request.providerType().trim()); value.setBaseUrl(blankToNull(request.baseUrl()));
        value.setCredentialRef(blankToNull(request.credentialRef())); value.setMaskedCredentialHint(blankToNull(request.maskedCredentialHint()));
        value.setTimeoutSeconds(request.timeoutSeconds() == null ? value.getTimeoutSeconds() : request.timeoutSeconds()); value.setMaxConcurrency(request.maxConcurrency() == null ? value.getMaxConcurrency() : request.maxConcurrency());
        if (request.config() != null) value.setConfigJson(Hashing.json(request.config())); if (request.enabled() != null) value.setEnabled(request.enabled() ? 1 : 0);
        value.setVersion(value.getVersion() + 1); value.setUpdatedAt(new Date()); providers.updateById(value); return requiredProvider(id);
    }

    @Transactional
    public void deleteProvider(Long id, Integer version) {
        AdsProvider value = requiredProvider(id); verifyVersion(value.getVersion(), version, "PROVIDER_VERSION_CONFLICT");
        if (models.count(Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getProviderId, id).eq(AdsModel::getDeleted, 0)) > 0)
            throw BizException.badRequest("PROVIDER_HAS_MODELS", "请先删除或迁移该 Provider 下的模型");
        value.setDeleted(1); value.setVersion(value.getVersion() + 1); value.setUpdatedAt(new Date()); providers.updateById(value);
    }

    public List<AdsModel> models(Long providerId) {
        var query = Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getDeleted, 0).orderByAsc(AdsModel::getSortNo).orderByAsc(AdsModel::getDisplayName);
        if (providerId != null) query.eq(AdsModel::getProviderId, providerId); return models.list(query);
    }

    @Transactional
    public AdsModel createModel(CatalogDtos.CreateModelRequest request) {
        requiredProvider(request.providerId());
        if (models.count(Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getProviderId, request.providerId()).eq(AdsModel::getModelCode, request.modelCode().trim()).eq(AdsModel::getDeleted, 0)) > 0)
            throw BizException.conflict("MODEL_CODE_EXISTS", "该 Provider 下的模型编码已存在");
        Date now = new Date(); AdsModel value = new AdsModel(); value.setProviderId(request.providerId()); value.setModelCode(request.modelCode().trim()); value.setDisplayName(request.displayName().trim()); value.setModelType(request.modelType().trim());
        value.setCapabilitiesJson(Hashing.json(request.capabilities() == null ? java.util.Map.of() : request.capabilities())); value.setDefaultParametersJson(Hashing.json(request.defaultParameters() == null ? java.util.Map.of() : request.defaultParameters()));
        value.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1); value.setSortNo(request.sortNo() == null ? 0 : request.sortNo()); value.setVersion(1); value.setDeleted(0); value.setCreatedAt(now); value.setUpdatedAt(now); models.save(value); return value;
    }

    @Transactional
    public AdsModel updateModel(Long id, CatalogDtos.UpdateModelRequest request) {
        AdsModel value = requiredModel(id); verifyVersion(value.getVersion(), request.version(), "MODEL_VERSION_CONFLICT");
        value.setDisplayName(request.displayName().trim()); value.setModelType(request.modelType().trim()); if (request.capabilities() != null) value.setCapabilitiesJson(Hashing.json(request.capabilities()));
        if (request.defaultParameters() != null) value.setDefaultParametersJson(Hashing.json(request.defaultParameters())); if (request.enabled() != null) value.setEnabled(request.enabled() ? 1 : 0);
        if (request.sortNo() != null) value.setSortNo(request.sortNo()); value.setVersion(value.getVersion() + 1); value.setUpdatedAt(new Date()); models.updateById(value); return requiredModel(id);
    }

    @Transactional
    public void deleteModel(Long id, Integer version) {
        AdsModel value = requiredModel(id); verifyVersion(value.getVersion(), version, "MODEL_VERSION_CONFLICT"); value.setDeleted(1); value.setVersion(value.getVersion() + 1); value.setUpdatedAt(new Date()); models.updateById(value);
    }

    public AdsProvider requiredProvider(Long id) { AdsProvider value = providers.getOne(Wrappers.<AdsProvider>lambdaQuery().eq(AdsProvider::getId, id).eq(AdsProvider::getDeleted, 0)); if (value == null) throw BizException.notFound("PROVIDER_NOT_FOUND", "Provider 不存在"); return value; }
    public AdsModel requiredModel(Long id) { AdsModel value = models.getOne(Wrappers.<AdsModel>lambdaQuery().eq(AdsModel::getId, id).eq(AdsModel::getDeleted, 0)); if (value == null) throw BizException.notFound("MODEL_NOT_FOUND", "模型不存在"); return value; }
    private static void verifyVersion(Integer actual, Integer expected, String code) { if (actual == null || !actual.equals(expected)) throw BizException.conflict(code, "资源已被其他修改覆盖，请刷新后重试"); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
