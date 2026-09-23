package com.ckrey.autobackworkflow.dialogue.api;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.dialogue.application.DialogueApplicationService;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DialogueController {
    private final DialogueApplicationService dialogue;

    public DialogueController(DialogueApplicationService dialogue) {
        this.dialogue = dialogue;
    }

    @GetMapping("/projects/{projectId}/segments")
    public ApiResponse<List<AdsDialogueSegment>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(dialogue.list(projectId));
    }

    @PutMapping("/segments/{id}")
    public ApiResponse<AdsDialogueSegment> update(@PathVariable Long id, @Valid @RequestBody SegmentDtos.UpdateSegmentRequest request) {
        return ApiResponse.ok(dialogue.update(id, request));
    }

    @PostMapping("/segments/{id}/split")
    public ApiResponse<List<AdsDialogueSegment>> split(@PathVariable Long id, @Valid @RequestBody SegmentDtos.SplitSegmentRequest request) {
        return ApiResponse.ok(dialogue.split(id, request));
    }

    @PostMapping("/segments/merge")
    public ApiResponse<List<AdsDialogueSegment>> merge(@Valid @RequestBody SegmentDtos.MergeSegmentsRequest request) {
        return ApiResponse.ok(dialogue.merge(request));
    }

    @PostMapping("/projects/{projectId}/segments/reorder")
    public ApiResponse<List<AdsDialogueSegment>> reorder(@PathVariable Long projectId, @Valid @RequestBody SegmentDtos.ReorderSegmentsRequest request) {
        return ApiResponse.ok(dialogue.reorder(projectId, request));
    }
}
