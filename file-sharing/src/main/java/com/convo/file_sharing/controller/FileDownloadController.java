package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.DownloadRecordDto;
import com.convo.file_sharing.dto.DownloadRegistrationDto;
import com.convo.file_sharing.security.CurrentUser;
import com.convo.file_sharing.service.FileDownloadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Beyond the shared /api/file-sharing service prefix, these two endpoints
// don't share a deeper URL prefix by design — recording a download is
// scoped under the session it happened in (mirrors
// SessionParticipantController's shape), while listing downloads is keyed
// by contentHash alone, independent of any one session, mirroring how
// getChainHistory works off contentHash too.
@RestController
@RequestMapping("/api/file-sharing")
public class FileDownloadController {

    private final FileDownloadService service;

    public FileDownloadController(FileDownloadService service) {
        this.service = service;
    }

    // userId in the body must match the caller's own authenticated
    // identity — see FileDownloadService.recordDownload.
    @PostMapping("/sessions/{sessionId}/downloads")
    public ResponseEntity<DownloadRecordDto> recordDownload(
            @PathVariable String sessionId,
            @Valid @RequestBody DownloadRegistrationDto request) {
        DownloadRecordDto response = service.recordDownload(sessionId, request, CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/downloads/{contentHash}")
    public ResponseEntity<List<DownloadRecordDto>> listDownloads(@PathVariable String contentHash) {
        return ResponseEntity.ok(service.listDownloads(contentHash));
    }
}
