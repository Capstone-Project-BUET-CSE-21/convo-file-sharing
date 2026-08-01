package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

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
        String signature
) {}