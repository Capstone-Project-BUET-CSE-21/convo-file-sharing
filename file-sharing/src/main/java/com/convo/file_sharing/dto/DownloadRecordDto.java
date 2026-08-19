package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DownloadRecordDto(
        UUID id,
        String sessionId,
        UUID userId,
        String contentHash,
        OffsetDateTime downloadedAt
) {}
