package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.security.CurrentUser;
import com.convo.file_sharing.service.TransferMetadataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfer/metadata")
public class TransferMetadataController {

    private final TransferMetadataService service;

    public TransferMetadataController(TransferMetadataService service) {
        this.service = service;
    }

    // 3.1: POST /api/transfer/metadata — senderId in the body must match the
    // caller's own authenticated identity (see TransferMetadataService).
    @PostMapping
    public ResponseEntity<MetadataResponseDto> create(@Valid @RequestBody MetadataRequestDto request) {
        MetadataResponseDto response = service.createPendingTransfer(request, CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 3.1 Task 3: PATCH /api/transfer/metadata/{transferId} — only the
    // sender who created this pending row may attach its hash/signature.
    @PatchMapping("/{transferId}")
    public ResponseEntity<MetadataResponseDto> attachHashAndSignature(
            @PathVariable UUID transferId,
            @Valid @RequestBody MetadataPatchDto patch) {
        MetadataResponseDto response = service.attachHashAndSignature(transferId, patch, CurrentUser.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{contentHash}")
    public ResponseEntity<java.util.List<com.convo.file_sharing.dto.ChainHistoryResponseDto>> getChainHistory(
            @PathVariable String contentHash) {
        return ResponseEntity.ok(service.getChainHistory(contentHash));
    }
}
