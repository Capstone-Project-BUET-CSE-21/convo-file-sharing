package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// `recipients` is who THIS hop's sender declared the file was going to.
//
// senderDisplayName is resolved server-side via UserLookupClient (a call to
// convo-backend's internal user-batch API), so the frontend gets a real
// name directly in this response instead of making a second, separate call
// to convo-backend itself. Null if convo-backend couldn't resolve the id
// (deleted/unknown user) or was unreachable — callers should already have
// a fallback label for a missing name.
public record ChainHistoryResponseDto(
        UUID transferId,
        String sessionId,
        String originSessionId,
        UUID senderId,
        String senderDisplayName,
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