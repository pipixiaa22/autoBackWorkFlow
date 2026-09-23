package com.ckrey.autobackworkflow.dialogue.application;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.dialogue.api.SegmentDtos;
import com.ckrey.autobackworkflow.domain.AdsAudioAsset;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import com.ckrey.autobackworkflow.service.AdsAudioAssetService;
import com.ckrey.autobackworkflow.service.AdsDialogueSegmentService;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DialogueApplicationService {
    private final AdsDialogueSegmentService segments;
    private final AdsAudioAssetService assets;
    private final ProjectApplicationService projects;

    public DialogueApplicationService(AdsDialogueSegmentService segments, AdsAudioAssetService assets,
                                      ProjectApplicationService projects) {
        this.segments = segments;
        this.assets = assets;
        this.projects = projects;
    }

    public List<AdsDialogueSegment> list(Long projectId) {
        projects.required(projectId);
        return segments.list(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getProjectId, projectId)
                .eq(AdsDialogueSegment::getDeleted, 0).orderByAsc(AdsDialogueSegment::getSegmentNo));
    }

    @Transactional
    public AdsDialogueSegment update(Long segmentId, SegmentDtos.UpdateSegmentRequest request) {
        AdsDialogueSegment current = required(segmentId);
        verifyVersion(current.getVersion(), request.version(), "DIALOGUE_VERSION_CONFLICT");
        String spoken = request.spokenText() == null ? current.getSpokenText() : request.spokenText();
        boolean audioStale = !safeEquals(current.getSpokenText(), spoken)
                || !safeEquals(current.getVoiceDirection(), request.voiceDirection() == null ? current.getVoiceDirection() : request.voiceDirection())
                || !safeEquals(current.getSpeed(), request.speed() == null ? current.getSpeed() : request.speed());
        LambdaUpdateWrapper<AdsDialogueSegment> update = Wrappers.<AdsDialogueSegment>lambdaUpdate()
                .eq(AdsDialogueSegment::getId, segmentId).eq(AdsDialogueSegment::getVersion, request.version())
                .set(AdsDialogueSegment::getSpokenText, spoken)
                .set(AdsDialogueSegment::getSubtitleText, request.subtitleText() == null ? current.getSubtitleText() : request.subtitleText())
                .set(AdsDialogueSegment::getSpeakerNameSnapshot, request.speakerName() == null ? current.getSpeakerNameSnapshot() : request.speakerName())
                .set(AdsDialogueSegment::getSemanticGroup, request.semanticGroup() == null ? current.getSemanticGroup() : request.semanticGroup())
                .set(AdsDialogueSegment::getEmotionJson, request.emotion() == null ? current.getEmotionJson() : Hashing.json(request.emotion()))
                .set(AdsDialogueSegment::getToneJson, request.tone() == null ? current.getToneJson() : Hashing.json(request.tone()))
                .set(AdsDialogueSegment::getPauseBeforeMs, request.pauseBeforeMs() == null ? current.getPauseBeforeMs() : request.pauseBeforeMs())
                .set(AdsDialogueSegment::getPauseAfterMs, request.pauseAfterMs() == null ? current.getPauseAfterMs() : request.pauseAfterMs())
                .set(AdsDialogueSegment::getSpeed, request.speed() == null ? current.getSpeed() : request.speed())
                .set(AdsDialogueSegment::getVolume, request.volume() == null ? current.getVolume() : request.volume())
                .set(AdsDialogueSegment::getEmphasisJson, request.emphasis() == null ? current.getEmphasisJson() : Hashing.json(request.emphasis()))
                .set(AdsDialogueSegment::getVoiceDirection, request.voiceDirection() == null ? current.getVoiceDirection() : request.voiceDirection())
                .set(AdsDialogueSegment::getRewriteMode, request.rewriteMode() == null ? current.getRewriteMode() : request.rewriteMode())
                .set(AdsDialogueSegment::getRewriteLevel, request.rewriteLevel() == null ? current.getRewriteLevel() : request.rewriteLevel())
                .set(AdsDialogueSegment::getRewriteReason, request.rewriteReason() == null ? current.getRewriteReason() : request.rewriteReason())
                .set(AdsDialogueSegment::getManualEdited, 1).set(AdsDialogueSegment::getAudioStale, audioStale ? 1 : current.getAudioStale())
                .set(AdsDialogueSegment::getVersion, request.version() + 1).set(AdsDialogueSegment::getUpdatedAt, new Date());
        if (!segments.update(update))
            throw BizException.conflict("DIALOGUE_VERSION_CONFLICT", "分段已被其他修改覆盖，请刷新后重试");
        if (audioStale) markAssetsStale(segmentId);
        return required(segmentId);
    }

    @Transactional
    public List<AdsDialogueSegment> split(Long segmentId, SegmentDtos.SplitSegmentRequest request) {
        AdsDialogueSegment first = required(segmentId);
        verifyVersion(first.getVersion(), request.version(), "DIALOGUE_VERSION_CONFLICT");
        String text = first.getOriginalText();
        if (request.splitOffset() >= text.length())
            throw BizException.badRequest("DIALOGUE_INVALID_SPLIT", "拆分位置必须在原始台词内部");
        String left = text.substring(0, request.splitOffset());
        String right = text.substring(request.splitOffset());
        if (left.isBlank() || right.isBlank())
            throw BizException.badRequest("DIALOGUE_INVALID_SPLIT", "拆分后不能出现空白分段");
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getProjectId, first.getProjectId())
                .gt(AdsDialogueSegment::getSegmentNo, first.getSegmentNo()).setSql("segment_no = segment_no + 1"));
        Date now = new Date();
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getId, first.getId())
                .eq(AdsDialogueSegment::getVersion, request.version()).set(AdsDialogueSegment::getOriginalText, left)
                .set(AdsDialogueSegment::getSpokenText, left).set(AdsDialogueSegment::getSubtitleText, left)
                .set(AdsDialogueSegment::getSourceEnd, first.getSourceStart() + request.splitOffset())
                .set(AdsDialogueSegment::getManualEdited, 1).set(AdsDialogueSegment::getAudioStale, 1)
                .set(AdsDialogueSegment::getVersion, request.version() + 1).set(AdsDialogueSegment::getUpdatedAt, now));
        AdsDialogueSegment second = new AdsDialogueSegment();
        copyForSplit(first, second, right, first.getSegmentNo() + 1, first.getSourceStart() + request.splitOffset(), first.getSourceEnd(), now);
        segments.save(second);
        markAssetsStale(segmentId);
        return list(first.getProjectId());
    }

    @Transactional
    public List<AdsDialogueSegment> merge(SegmentDtos.MergeSegmentsRequest request) {
        AdsDialogueSegment first = required(request.firstSegmentId());
        AdsDialogueSegment second = required(request.secondSegmentId());
        verifyVersion(first.getVersion(), request.firstVersion(), "DIALOGUE_VERSION_CONFLICT");
        verifyVersion(second.getVersion(), request.secondVersion(), "DIALOGUE_VERSION_CONFLICT");
        if (!first.getProjectId().equals(second.getProjectId()) || second.getSegmentNo() != first.getSegmentNo() + 1) {
            throw BizException.badRequest("DIALOGUE_NOT_CONTIGUOUS", "只能合并同一项目中相邻的分段");
        }
        Date now = new Date();
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getId, first.getId())
                .eq(AdsDialogueSegment::getVersion, first.getVersion()).set(AdsDialogueSegment::getOriginalText, first.getOriginalText() + second.getOriginalText())
                .set(AdsDialogueSegment::getSpokenText, first.getSpokenText() + second.getSpokenText())
                .set(AdsDialogueSegment::getSubtitleText, first.getSubtitleText() + second.getSubtitleText())
                .set(AdsDialogueSegment::getSourceEnd, second.getSourceEnd()).set(AdsDialogueSegment::getPauseAfterMs, second.getPauseAfterMs())
                .set(AdsDialogueSegment::getManualEdited, 1).set(AdsDialogueSegment::getAudioStale, 1)
                .set(AdsDialogueSegment::getVersion, first.getVersion() + 1).set(AdsDialogueSegment::getUpdatedAt, now));
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getId, second.getId())
                .eq(AdsDialogueSegment::getVersion, second.getVersion()).set(AdsDialogueSegment::getDeleted, 1));
        segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getProjectId, first.getProjectId())
                .gt(AdsDialogueSegment::getSegmentNo, second.getSegmentNo()).setSql("segment_no = segment_no - 1"));
        markAssetsStale(first.getId());
        markAssetsStale(second.getId());
        return list(first.getProjectId());
    }

    @Transactional
    public List<AdsDialogueSegment> reorder(Long projectId, SegmentDtos.ReorderSegmentsRequest request) {
        AdsProject project = projects.required(projectId);
        verifyVersion(project.getVersion(), request.projectVersion(), "PROJECT_VERSION_CONFLICT");
        List<AdsDialogueSegment> current = list(projectId);
        if (current.size() != request.segmentIds().size() || !current.stream().map(AdsDialogueSegment::getId).collect(java.util.stream.Collectors.toSet()).equals(new java.util.HashSet<>(request.segmentIds())))
            throw BizException.badRequest("DIALOGUE_INVALID_REORDER", "排序列表必须包含项目内全部分段且不可重复");
        for (int i = 0; i < request.segmentIds().size(); i++)
            segments.update(Wrappers.<AdsDialogueSegment>lambdaUpdate().eq(AdsDialogueSegment::getId, request.segmentIds().get(i)).set(AdsDialogueSegment::getSegmentNo, i + 1));
        return list(projectId);
    }

    public AdsDialogueSegment required(Long id) {
        AdsDialogueSegment result = segments.getOne(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getId, id).eq(AdsDialogueSegment::getDeleted, 0));
        if (result == null) throw BizException.notFound("DIALOGUE_NOT_FOUND", "台词分段不存在");
        return result;
    }

    private void markAssetsStale(Long segmentId) {
        assets.update(Wrappers.<AdsAudioAsset>lambdaUpdate().eq(AdsAudioAsset::getSegmentId, segmentId).eq(AdsAudioAsset::getDeleted, 0).set(AdsAudioAsset::getStatus, "STALE"));
    }

    private static void verifyVersion(Integer actual, Integer wanted, String code) {
        if (actual == null || !actual.equals(wanted))
            throw BizException.conflict(code, "资源已被其他修改覆盖，请刷新后重试");
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void copyForSplit(AdsDialogueSegment source, AdsDialogueSegment target, String text, int no, int start, Integer end, Date now) {
        target.setProjectId(source.getProjectId());
        target.setSegmentNo(no);
        target.setSpeakerId(source.getSpeakerId());
        target.setSpeakerNameSnapshot(source.getSpeakerNameSnapshot());
        target.setSemanticGroup(source.getSemanticGroup());
        target.setSourceStart(start);
        target.setSourceEnd(end);
        target.setOriginalText(text);
        target.setSpokenText(text);
        target.setSubtitleText(text);
        target.setEmotionJson(source.getEmotionJson());
        target.setToneJson(source.getToneJson());
        target.setSpeed(source.getSpeed());
        target.setVolume(source.getVolume());
        target.setPauseBeforeMs(0);
        target.setPauseAfterMs(source.getPauseAfterMs());
        target.setEmphasisJson(source.getEmphasisJson());
        target.setVoiceDirection(source.getVoiceDirection());
        target.setRewriteMode(source.getRewriteMode());
        target.setRewriteLevel("MANUAL");
        target.setManualEdited(1);
        target.setAudioStale(1);
        target.setVersion(1);
        target.setCreatedAt(now);
        target.setUpdatedAt(now);
        target.setDeleted(0);
    }
}
