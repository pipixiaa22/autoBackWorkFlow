package com.ckrey.autobackworkflow.export.api;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsExportRecord;
import com.ckrey.autobackworkflow.export.application.ExportApplicationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ExportController {
    private final ExportApplicationService exports;

    public ExportController(ExportApplicationService exports) {
        this.exports = exports;
    }

    @PostMapping("/projects/{projectId}/exports/srt")
    public ApiResponse<AdsExportRecord> srt(@PathVariable Long projectId, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ApiResponse.ok(exports.exportSrt(projectId, key));
    }

    @PostMapping("/projects/{projectId}/exports/package")
    public ApiResponse<AdsExportRecord> pack(@PathVariable Long projectId, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ApiResponse.ok(exports.exportPackage(projectId, key));
    }

    @GetMapping("/projects/{projectId}/exports")
    public ApiResponse<com.ckrey.autobackworkflow.common.api.CursorPage<AdsExportRecord>> list(@PathVariable Long projectId, @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(exports.list(projectId, cursor, limit));
    }

    @GetMapping("/exports/{id}")
    public ApiResponse<AdsExportRecord> get(@PathVariable Long id) {
        return ApiResponse.ok(exports.required(id));
    }

    @GetMapping("/exports/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        AdsExportRecord r = exports.required(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.getFileName() + "\"").contentType(MediaType.APPLICATION_OCTET_STREAM).body(exports.download(id));
    }
}
