package com.ckrey.autobackworkflow.project.api;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectApplicationService projects;
    public ProjectController(ProjectApplicationService projects) { this.projects = projects; }

    @PostMapping
    public ApiResponse<AdsProject> create(@Valid @RequestBody ProjectDtos.CreateProjectRequest request) {
        return ApiResponse.ok(projects.create(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<ProjectDtos.ProjectView> detail(@PathVariable Long id) { return ApiResponse.ok(projects.detail(id)); }
    @PutMapping("/{id}")
    public ApiResponse<AdsProject> update(@PathVariable Long id, @Valid @RequestBody ProjectDtos.UpdateProjectRequest request) {
        return ApiResponse.ok(projects.update(id, request));
    }
}
