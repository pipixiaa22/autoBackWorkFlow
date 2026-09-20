package com.ckrey.autobackworkflow.ai.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.ai.api.AnalysisDtos;
import com.ckrey.autobackworkflow.ai.llm.LlmProvider;
import com.ckrey.autobackworkflow.ai.llm.LlmProviderRegistry;
import com.ckrey.autobackworkflow.ai.model.AnalysisCandidate;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.AdsAnalysisRun;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.domain.AdsProvider;
import com.ckrey.autobackworkflow.domain.AdsModel;
import com.ckrey.autobackworkflow.domain.AdsSkillVersion;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import com.ckrey.autobackworkflow.service.AdsAnalysisRunService;
import com.ckrey.autobackworkflow.service.AdsDialogueSegmentService;
import com.ckrey.autobackworkflow.service.AdsProviderService;
import com.ckrey.autobackworkflow.service.AdsModelService;
import com.ckrey.autobackworkflow.skill.application.SkillApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisApplicationService {
    private final ProjectApplicationService projects; private final SkillApplicationService skills;
    private final AdsAnalysisRunService runs; private final AdsDialogueSegmentService segments;
    private final AdsProviderService providers; private final AdsModelService models;
    private final LlmProviderRegistry llmProviders; private final ObjectMapper mapper;
    public AnalysisApplicationService(ProjectApplicationService projects, SkillApplicationService skills, AdsAnalysisRunService runs,
                                      AdsDialogueSegmentService segments, AdsProviderService providers, AdsModelService models,
                                      LlmProviderRegistry llmProviders, ObjectMapper mapper) {
        this.projects=projects; this.skills=skills; this.runs=runs; this.segments=segments; this.providers=providers;
        this.models=models; this.llmProviders=llmProviders; this.mapper=mapper;
    }
    @Transactional public AdsAnalysisRun create(Long projectId, AnalysisDtos.CreateAnalysisRequest request) {
        AdsProject project = projects.required(projectId); AdsSkillVersion skill = skills.published(request.skillVersionId()); Date now = new Date();
        SelectedLlm selected = selectProvider(request.providerId(), request.modelId());
        AdsAnalysisRun run = new AdsAnalysisRun(); run.setRunNo("AN-"+UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()); run.setProjectId(projectId); run.setProjectVersion(project.getVersion()); run.setSkillVersionId(skill.getId()); run.setProviderId(request.providerId()); run.setModelId(request.modelId()); run.setProviderCodeSnapshot(selected.provider().providerCode()); run.setModelCodeSnapshot(selected.modelCode()); run.setRewriteMode(request.rewriteMode() == null ? "STRICT" : request.rewriteMode()); run.setBackgroundSnapshot(project.getStoryBackground()); run.setDialogueSnapshot(project.getOriginalDialogue()); run.setInputHash(Hashing.sha256((project.getStoryBackground()==null?"":project.getStoryBackground())+"\n"+project.getOriginalDialogue())); run.setPromptHash(Hashing.sha256(skill.getSystemPromptTemplate())); run.setModelParametersJson(skill.getModelParametersJson()); run.setStatus("RUNNING"); run.setVersion(1); run.setCreatedAt(now); run.setUpdatedAt(now); run.setStartedAt(now); runs.save(run);
        try {
            AnalysisCandidate candidate = selected.provider().analyse(new LlmProvider.LlmAnalysisCommand(project.getStoryBackground(), project.getOriginalDialogue(), run.getRewriteMode(), skill.getSystemPromptTemplate(), selected.modelCode()));
            if(candidate.segments().isEmpty()) throw BizException.badRequest("SKILL_INVALID_OUTPUT", "分析结果未包含可用台词分段");
            run.setRawResponseJson(Hashing.json(candidate)); run.setCandidateResultJson(Hashing.json(candidate)); run.setValidationWarningsJson("[]"); run.setStatus("SUCCEEDED"); run.setFinishedAt(new Date()); run.setUpdatedAt(new Date()); runs.updateById(run);
        } catch (RuntimeException ex) {
            run.setStatus("FAILED"); run.setErrorCode(ex instanceof BizException business ? business.getCode() : "SKILL_ANALYSIS_FAILED"); run.setErrorMessage("分析服务未能生成有效结果"); run.setFinishedAt(new Date()); runs.updateById(run); if (ex instanceof BizException business) throw business;
        }
        return required(projectId, run.getId());
    }
    public AdsAnalysisRun get(Long projectId, Long runId) { return required(projectId, runId); }
    @Transactional public List<AdsDialogueSegment> apply(Long projectId, Long runId, AnalysisDtos.ApplyAnalysisRequest request) {
        AdsProject project = projects.required(projectId); AdsAnalysisRun run = required(projectId,runId);
        if (!"SUCCEEDED".equals(run.getStatus())) throw BizException.badRequest("SKILL_RUN_NOT_READY", "仅成功的分析运行可以应用");
        if (!project.getVersion().equals(request.projectVersion()) || !project.getVersion().equals(run.getProjectVersion())) throw BizException.conflict("PROJECT_VERSION_CONFLICT", "项目在分析后已发生变化，请重新分析");
        if (segments.count(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getProjectId, projectId).eq(AdsDialogueSegment::getDeleted,0).eq(AdsDialogueSegment::getManualEdited,1)) > 0) throw BizException.conflict("DIALOGUE_MANUAL_EDIT_CONFLICT", "存在人工编辑分段，不能静默覆盖");
        AnalysisCandidate candidate = readCandidate(run.getCandidateResultJson());
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getProjectId,projectId).eq(AdsDialogueSegment::getDeleted,0).set(AdsDialogueSegment::getDeleted,1));
        Date now = new Date(); for (AnalysisCandidate.CandidateSegment item : candidate.segments()) segments.save(toSegment(projectId, runId, item, now));
        run.setStatus("APPLIED"); run.setAppliedAt(now); run.setVersion(run.getVersion()+1); runs.updateById(run); return segments.list(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getProjectId,projectId).eq(AdsDialogueSegment::getDeleted,0).orderByAsc(AdsDialogueSegment::getSegmentNo));
    }
    private AnalysisCandidate readCandidate(Object value) { try { return mapper.readValue(value instanceof String s ? s : mapper.writeValueAsString(value), AnalysisCandidate.class); } catch (Exception ex) { throw BizException.badRequest("SKILL_INVALID_OUTPUT", "保存的分析候选结果无法读取"); } }
    private SelectedLlm selectProvider(Long providerId, Long modelId) {
        if (providerId == null && modelId == null) return new SelectedLlm(llmProviders.getRequired("local-rule"), null);
        if (providerId == null || modelId == null) throw BizException.badRequest("PROVIDER_INVALID_MODEL", "providerId 和 modelId 必须同时提供");
        AdsProvider provider = providers.getById(providerId); AdsModel model = models.getById(modelId);
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1 || model == null
                || model.getEnabled() == null || model.getEnabled() != 1 || !provider.getId().equals(model.getProviderId())
                || !"LLM".equalsIgnoreCase(provider.getProviderType()) || !"LLM".equalsIgnoreCase(model.getModelType())) {
            throw BizException.badRequest("PROVIDER_INVALID_MODEL", "LLM Provider 或模型不可用");
        }
        return new SelectedLlm(llmProviders.getRequired(provider.getProviderCode()), model.getModelCode());
    }
    private AdsAnalysisRun required(Long projectId, Long runId) { AdsAnalysisRun run=runs.getOne(Wrappers.<AdsAnalysisRun>lambdaQuery().eq(AdsAnalysisRun::getId,runId).eq(AdsAnalysisRun::getProjectId,projectId)); if(run==null) throw BizException.notFound("SKILL_RUN_NOT_FOUND", "分析运行不存在"); return run; }
    private AdsDialogueSegment toSegment(Long projectId, Long runId, AnalysisCandidate.CandidateSegment item, Date now) { AdsDialogueSegment v=new AdsDialogueSegment(); v.setProjectId(projectId);v.setSegmentNo(item.segmentNo());v.setSpeakerNameSnapshot(item.speaker());v.setSemanticGroup(item.semanticGroup());v.setSourceStart(item.sourceStart());v.setSourceEnd(item.sourceEnd());v.setOriginalText(item.originalText());v.setSpokenText(item.spokenText());v.setSubtitleText(item.subtitleText());v.setEmotionJson(Hashing.json(item.emotion()));v.setToneJson(Hashing.json(item.tone()));v.setSpeed(item.speed());v.setVolume(item.volume());v.setPauseBeforeMs(item.pauseBeforeMs());v.setPauseAfterMs(item.pauseAfterMs());v.setEmphasisJson(Hashing.json(item.emphasis()));v.setVoiceDirection(item.voiceDirection());v.setRewriteMode(item.rewriteMode());v.setRewriteLevel(item.rewriteLevel());v.setRewriteReason(item.rewriteReason());v.setManualEdited(0);v.setAudioStale(1);v.setAnalysisRunId(runId);v.setVersion(1);v.setCreatedAt(now);v.setUpdatedAt(now);v.setDeleted(0);return v; }
    private record SelectedLlm(LlmProvider provider, String modelCode) { }
}
