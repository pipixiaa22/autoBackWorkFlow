package com.ckrey.autobackworkflow.skill.api;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsSkill;
import com.ckrey.autobackworkflow.domain.AdsSkillVersion;
import com.ckrey.autobackworkflow.skill.application.SkillApplicationService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SkillController {
    private final SkillApplicationService skills;

    public SkillController(SkillApplicationService skills) {
        this.skills = skills;
    }

    @GetMapping("/skills")
    public ApiResponse<List<AdsSkill>> list() {
        return ApiResponse.ok(skills.list());
    }

    @GetMapping("/skills/{id}")
    public ApiResponse<SkillDtos.SkillDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(skills.get(id));
    }

    @PostMapping("/skills")
    public ApiResponse<SkillDtos.SkillDetail> createSkill(@Valid @RequestBody SkillDtos.SaveSkillRequest request) {
        return ApiResponse.ok(skills.create(request));
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<SkillDtos.SkillDetail> updateSkill(@PathVariable Long id,
                                                          @Valid @RequestBody SkillDtos.SaveSkillRequest request) {
        return ApiResponse.ok(skills.update(id, request));
    }

    @DeleteMapping("/skills/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long id) {
        skills.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/skills/{id}/versions")
    public ApiResponse<List<AdsSkillVersion>> versions(@PathVariable Long id) {
        return ApiResponse.ok(skills.versions(id));
    }

    @PostMapping("/skills/{id}/versions")
    public ApiResponse<AdsSkillVersion> create(@PathVariable Long id, @Valid @RequestBody SkillDtos.CreateSkillVersionRequest request) {
        return ApiResponse.ok(skills.createVersion(id, request));
    }

    @PostMapping("/skill-versions/{id}/test")
    public ApiResponse<Map<String, Object>> test(@PathVariable Long id) {
        return ApiResponse.ok(skills.test(id));
    }

    @PostMapping("/skill-versions/{id}/publish")
    public ApiResponse<AdsSkillVersion> publish(@PathVariable Long id) {
        return ApiResponse.ok(skills.publish(id));
    }
}
