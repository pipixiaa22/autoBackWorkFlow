package com.ckrey.autobackworkflow.character.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.catalog.application.CatalogApplicationService;
import com.ckrey.autobackworkflow.character.api.CharacterDtos;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.AdsCharacter;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import com.ckrey.autobackworkflow.service.AdsCharacterService;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterApplicationService {
    private final AdsCharacterService characters;
    private final ProjectApplicationService projects;
    private final CatalogApplicationService catalog;

    public CharacterApplicationService(AdsCharacterService characters, ProjectApplicationService projects, CatalogApplicationService catalog) {
        this.characters = characters;
        this.projects = projects;
        this.catalog = catalog;
    }

    public List<AdsCharacter> list(Long projectId) {
        projects.required(projectId);
        return characters.list(Wrappers.<AdsCharacter>lambdaQuery().eq(AdsCharacter::getProjectId, projectId).eq(AdsCharacter::getDeleted, 0).orderByAsc(AdsCharacter::getSortNo).orderByAsc(AdsCharacter::getName));
    }

    @Transactional
    public AdsCharacter create(Long projectId, CharacterDtos.CreateCharacterRequest request) {
        projects.required(projectId);
        verifyVoiceBinding(request.defaultProviderId(), request.defaultModelId());
        if (characters.count(Wrappers.<AdsCharacter>lambdaQuery().eq(AdsCharacter::getProjectId, projectId).eq(AdsCharacter::getCharacterCode, request.characterCode().trim()).eq(AdsCharacter::getDeleted, 0)) > 0)
            throw BizException.conflict("CHARACTER_CODE_EXISTS", "项目内角色编码已存在");
        Date now = new Date();
        AdsCharacter value = new AdsCharacter();
        value.setProjectId(projectId);
        value.setCharacterCode(request.characterCode().trim());
        value.setName(request.name().trim());
        value.setDescription(blankToNull(request.description()));
        value.setDefaultProviderId(request.defaultProviderId());
        value.setDefaultModelId(request.defaultModelId());
        value.setDefaultVoiceId(blankToNull(request.defaultVoiceId()));
        value.setVoiceConfigJson(Hashing.json(request.voiceConfig() == null ? java.util.Map.of() : request.voiceConfig()));
        value.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        value.setVersion(1);
        value.setDeleted(0);
        value.setCreatedAt(now);
        value.setUpdatedAt(now);
        characters.save(value);
        return value;
    }

    @Transactional
    public AdsCharacter update(Long id, CharacterDtos.UpdateCharacterRequest request) {
        AdsCharacter value = required(id);
        verifyVersion(value.getVersion(), request.version());
        verifyVoiceBinding(request.defaultProviderId(), request.defaultModelId());
        value.setName(request.name().trim());
        value.setDescription(blankToNull(request.description()));
        value.setDefaultProviderId(request.defaultProviderId());
        value.setDefaultModelId(request.defaultModelId());
        value.setDefaultVoiceId(blankToNull(request.defaultVoiceId()));
        if (request.voiceConfig() != null) value.setVoiceConfigJson(Hashing.json(request.voiceConfig()));
        if (request.sortNo() != null) value.setSortNo(request.sortNo());
        value.setVersion(value.getVersion() + 1);
        value.setUpdatedAt(new Date());
        characters.updateById(value);
        return required(id);
    }

    @Transactional
    public void delete(Long id, Integer version) {
        AdsCharacter value = required(id);
        verifyVersion(value.getVersion(), version);
        value.setDeleted(1);
        value.setVersion(value.getVersion() + 1);
        value.setUpdatedAt(new Date());
        characters.updateById(value);
    }

    private void verifyVoiceBinding(Long providerId, Long modelId) {
        if (providerId == null && modelId == null) return;
        if (providerId == null || modelId == null)
            throw BizException.badRequest("CHARACTER_INVALID_VOICE_BINDING", "默认 Provider 与模型必须同时提供");
        var provider = catalog.requiredProvider(providerId);
        AdsModel model = catalog.requiredModel(modelId);
        if (provider.getEnabled() != 1 || model.getEnabled() != 1 || !providerId.equals(model.getProviderId()) || !"AUDIO".equalsIgnoreCase(provider.getProviderType()) || !"AUDIO".equalsIgnoreCase(model.getModelType()))
            throw BizException.badRequest("CHARACTER_INVALID_VOICE_BINDING", "默认音频 Provider 或模型不可用");
    }

    private AdsCharacter required(Long id) {
        AdsCharacter value = characters.getOne(Wrappers.<AdsCharacter>lambdaQuery().eq(AdsCharacter::getId, id).eq(AdsCharacter::getDeleted, 0));
        if (value == null) throw BizException.notFound("CHARACTER_NOT_FOUND", "角色不存在");
        return value;
    }

    private static void verifyVersion(Integer actual, Integer expected) {
        if (actual == null || !actual.equals(expected))
            throw BizException.conflict("CHARACTER_VERSION_CONFLICT", "资源已被其他修改覆盖，请刷新后重试");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
