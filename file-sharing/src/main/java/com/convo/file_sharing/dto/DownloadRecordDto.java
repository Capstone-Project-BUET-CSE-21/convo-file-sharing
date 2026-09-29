package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// userDisplayName is resolved server-side via UserLookupClient — see
// ChainHistoryResponseDto's doc comment for why (same reasoning, same
// null-if-unresolvable contract).
public record DownloadRecordDto(
        UUID id,
        String sessionId,
        UUID userId,
        String userDisplayName,
        String contentHash,
        OffsetDateTime downloadedAt
) {}
