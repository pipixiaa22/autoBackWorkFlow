package com.ckrey.autobackworkflow.project.application;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.project.api.ProjectDtos;
import com.ckrey.autobackworkflow.service.AdsDialogueSegmentService;
import com.ckrey.autobackworkflow.service.AdsProjectService;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApplicationService {
    private final AdsProjectService projects;
    private final AdsDialogueSegmentService segments;

    public ProjectApplicationService(AdsProjectService projects, AdsDialogueSegmentService segments) {
        this.projects = projects;
        this.segments = segments;
    }

    @Transactional
    public AdsProject create(ProjectDtos.CreateProjectRequest request) {
        Date now = new Date();
        AdsProject project = new AdsProject();
        project.setProjectNo("ADS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        project.setName(request.name().trim());
        project.setStoryBackground(blankToNull(request.storyBackground()));
        project.setOriginalDialogue(request.originalDialogue());
        project.setOutputMode(request.outputMode() == null ? "JIAN_YING_TTS" : request.outputMode());
        project.setDefaultSkillVersionId(request.defaultSkillVersionId());
        project.setStatus("DRAFT");
        project.setAnalysisStale(0);
        project.setVersion(1);
        project.setDeleted(0);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projects.save(project);
        return project;
    }

    public ProjectDtos.ProjectView detail(Long id) {
        AdsProject project = required(id);
        List<AdsDialogueSegment> rows = segments.list(Wrappers.<AdsDialogueSegment>lambdaQuery()
                .eq(AdsDialogueSegment::getProjectId, id)
                .eq(AdsDialogueSegment::getDeleted, 0)
                .orderByAsc(AdsDialogueSegment::getSegmentNo));
        return view(project, List.copyOf(rows));
    }

    @Transactional
    public AdsProject update(Long id, ProjectDtos.UpdateProjectRequest request) {
        AdsProject current = required(id);
        if (!current.getVersion().equals(request.version())) {
            throw BizException.conflict("PROJECT_VERSION_CONFLICT", "项目已被其他修改覆盖，请刷新后重试");
        }
        boolean stale = !equalsNullable(current.getStoryBackground(), blankToNull(request.storyBackground()));
        LambdaUpdateWrapper<AdsProject> update = Wrappers.<AdsProject>lambdaUpdate()
                .eq(AdsProject::getId, id).eq(AdsProject::getVersion, request.version())
                .set(AdsProject::getName, request.name().trim())
                .set(AdsProject::getStoryBackground, blankToNull(request.storyBackground()))
                .set(AdsProject::getOutputMode, request.outputMode() == null ? current.getOutputMode() : request.outputMode())
                .set(AdsProject::getAnalysisStale, stale ? 1 : current.getAnalysisStale())
                .set(AdsProject::getVersion, request.version() + 1)
                .set(AdsProject::getUpdatedAt, new Date());
        if (!projects.update(update)) throw BizException.conflict("PROJECT_VERSION_CONFLICT", "项目已被其他修改覆盖，请刷新后重试");
        return required(id);
    }

    public AdsProject required(Long id) {
        AdsProject project = projects.getOne(Wrappers.<AdsProject>lambdaQuery()
                .eq(AdsProject::getId, id).eq(AdsProject::getDeleted, 0));
        if (project == null) throw BizException.notFound("PROJECT_NOT_FOUND", "项目不存在");
        return project;
    }

    private ProjectDtos.ProjectView view(AdsProject project, List<?> rows) {
        return new ProjectDtos.ProjectView(project.getId(), project.getProjectNo(), project.getName(),
                project.getStoryBackground(), project.getOriginalDialogue(), project.getOutputMode(),
                project.getDefaultSkillVersionId(), project.getStatus(), project.getAnalysisStale() != null
                && project.getAnalysisStale() == 1, project.getVersion(), instant(project.getCreatedAt()),
                instant(project.getUpdatedAt()), rows);
    }
    private static Date instant(Date date) { return date; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static boolean equalsNullable(String a, String b) { return a == null ? b == null : a.equals(b); }
}
