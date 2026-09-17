package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// `recipients` is who THIS hop's sender declared the file was going to.
// makeIsAuthorizedHop (frontend) checks a later hop's senderId against its
// immediate ancestor's `recipients` list here — real per-file authorization
// instead of "was in the same session at some point."
public record ChainHistoryResponseDto(
        UUID transferId,
        String sessionId,
        String originSessionId,
        UUID senderId,
        String fileName,
        Long fileSize,
        String mimeType,
        OffsetDateTime timestamp,
        String previousHash,
        String contentHash,
        String fileHash,
        String signature,
        List<UUID> recipients
) {}