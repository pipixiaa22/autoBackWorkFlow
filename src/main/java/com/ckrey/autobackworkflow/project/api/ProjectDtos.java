package com.ckrey.autobackworkflow.project.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;

public final class ProjectDtos {
    private ProjectDtos() { }

    public record CreateProjectRequest(
            @NotBlank(message = "项目名称不能为空") @Size(max = 120) String name,
            @Size(max = 20000) String storyBackground,
            @NotBlank(message = "原始台词不能为空") @Size(max = 100000) String originalDialogue,
            String outputMode, Long defaultSkillVersionId) { }

    public record UpdateProjectRequest(
            @NotBlank(message = "项目名称不能为空") @Size(max = 120) String name,
            @Size(max = 20000) String storyBackground,
            String outputMode,
            @NotNull(message = "版本号不能为空") Integer version) { }

    public record ProjectView(Long id, String projectNo, String name, String storyBackground,
                              String originalDialogue, String outputMode, Long defaultSkillVersionId,
                              String status, boolean analysisStale, Integer version, Date createdAt,
                              Date updatedAt, List<?> segments) { }
}
