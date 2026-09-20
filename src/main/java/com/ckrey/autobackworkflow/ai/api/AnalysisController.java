package com.ckrey.autobackworkflow.ai.api;

import com.ckrey.autobackworkflow.ai.application.AnalysisApplicationService;
import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsAnalysisRun;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/projects/{projectId}/analysis-runs")
public class AnalysisController {
    private final AnalysisApplicationService analysis; public AnalysisController(AnalysisApplicationService analysis) { this.analysis=analysis; }
    @PostMapping public ApiResponse<AdsAnalysisRun> create(@PathVariable Long projectId,@Valid @RequestBody AnalysisDtos.CreateAnalysisRequest request) { return ApiResponse.ok(analysis.create(projectId,request)); }
    @GetMapping("/{runId}") public ApiResponse<AdsAnalysisRun> get(@PathVariable Long projectId,@PathVariable Long runId) { return ApiResponse.ok(analysis.get(projectId,runId)); }
    @PostMapping("/{runId}/apply") public ApiResponse<List<AdsDialogueSegment>> apply(@PathVariable Long projectId,@PathVariable Long runId,@Valid @RequestBody AnalysisDtos.ApplyAnalysisRequest request) { return ApiResponse.ok(analysis.apply(projectId,runId,request)); }
}
