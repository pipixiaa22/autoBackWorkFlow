package com.ckrey.autobackworkflow.project.api;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectApplicationService projects;

    public ProjectController(ProjectApplicationService projects) {
        this.projects = projects;
    }

    @GetMapping("/query")
    public ApiResponse<List<AdsProject>> query() {
        return ApiResponse.ok(projects.list());
    }

    @GetMapping("/trash")
    public ApiResponse<com.ckrey.autobackworkflow.common.api.CursorPage<AdsProject>> trash(
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(projects.trash(cursor, limit));
    }

    @PostMapping
    public ApiResponse<AdsProject> create(@Valid @RequestBody ProjectDtos.CreateProjectRequest request) {
        return ApiResponse.ok(projects.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDtos.ProjectView> detail(@PathVariable Long id) {
        return ApiResponse.ok(projects.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdsProject> update(@PathVariable Long id, @Valid @RequestBody ProjectDtos.UpdateProjectRequest request) {
        return ApiResponse.ok(projects.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Integer version) {
        projects.delete(id, version);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<AdsProject> restore(@PathVariable Long id) {
        return ApiResponse.ok(projects.restore(id));
    }
}
