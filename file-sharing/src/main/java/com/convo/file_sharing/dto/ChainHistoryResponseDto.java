package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// One completed share of a file. previousHash is the fileHash of the share
// this one continues — the latest earlier share that named this sender as a
// recipient — or null if the sender never received this content through
// Convo (they introduced it themselves). A file's history is therefore a
// forest: every null-previousHash entry starts its own tree.
//
// Display names are resolved server-side via UserLookupClient (a call to
// convo-backend's internal user-batch API) and are null if convo-backend
// couldn't resolve an id or was unreachable.
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
        List<RecipientDto> recipients
) {}
