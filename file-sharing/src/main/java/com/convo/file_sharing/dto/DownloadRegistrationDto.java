package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// POST /api/file-sharing/sessions/{sessionId}/downloads body. userId must match the
// caller's own authenticated identity (see FileDownloadService).
public record DownloadRegistrationDto(
        @NotNull UUID userId,
        @NotBlank String contentHash
) {}
