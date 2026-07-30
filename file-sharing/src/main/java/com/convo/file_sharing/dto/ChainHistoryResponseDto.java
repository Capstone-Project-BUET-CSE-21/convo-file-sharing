package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Extended: added fileHash + signature. Without these, a trace-screen hop
// could only be checked for structural consistency (does this row belong
// to the content hash being traced) — not whether the sender actually
// signed it. Both already exist on TransferMetadata (entity/TransferMetadata.java,
// populated via the PATCH in attachHashAndSignature); this just surfaces them
// on the read side too. See identity/traceVerification.js's makeVerifyHop on
// the frontend — it already checks for these fields and takes the full
// signature-verification branch once they're present, no frontend change needed.
public record ChainHistoryResponseDto(
        UUID transferId,
        UUID sessionId,
        UUID originSessionId,
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