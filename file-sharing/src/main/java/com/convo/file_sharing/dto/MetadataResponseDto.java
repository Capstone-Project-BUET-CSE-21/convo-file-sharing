package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Field values here must match section 0.1's provenance block exactly —
// this response IS the unsigned metadata block the client will canonicalize,
// hash, and sign. Don't add/rename fields without updating section 0 for everyone.
public record MetadataResponseDto(
        UUID transferId,
        UUID sessionId,
        UUID senderId,
        String fileName,
        Long fileSize,
        String mimeType,
        OffsetDateTime timestamp,
        String previousHash
) {}
