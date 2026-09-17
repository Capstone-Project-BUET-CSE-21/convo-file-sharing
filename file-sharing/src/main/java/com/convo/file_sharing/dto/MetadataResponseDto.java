package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// The first eight fields here must match section 0.1's provenance block
// exactly — this response IS the unsigned metadata block the client will
// canonicalize, hash, and sign. Don't add/rename any of THOSE fields
// without updating section 0 for everyone.
//
// `recipients` is deliberately NOT part of that signed block — it's
// server-side ACL metadata only (see TransferRecipient), added here purely
// so the client can see back what it just registered. Canonicalizer.java /
// canonicalize.js both hash a fixed, explicit 8-key whitelist, so this
// field is safely ignored by both sides' signing logic.
public record MetadataResponseDto(
        UUID transferId,
        String sessionId,
        UUID senderId,
        String fileName,
        Long fileSize,
        String mimeType,
        OffsetDateTime timestamp,
        String previousHash,
        List<UUID> recipients
) {}
