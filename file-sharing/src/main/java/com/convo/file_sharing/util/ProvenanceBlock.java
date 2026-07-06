package com.convo.file_sharing.util;

import java.util.UUID;

/**
 * Section 0.1's unsigned provenance block. `timestamp` is kept as the exact
 * ISO-8601 string that was put on the wire (not an OffsetDateTime re-formatted
 * later), so canonicalization always hashes/signs the literal string bytes
 * the client saw — re-deriving a formatted string from a parsed OffsetDateTime
 * risks a different string representation (e.g. offset vs "Z", trailing
 * zeros in fractional seconds) than what was actually transmitted.
 */
public record ProvenanceBlock(
        UUID transferId,
        UUID sessionId,
        UUID senderId,
        String fileName,
        Long fileSize,
        String mimeType,
        String timestamp,
        String previousHash
) {}
