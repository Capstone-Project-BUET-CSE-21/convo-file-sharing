package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Backs isAuthorizedHop (identity/traceVerification.js): "was this
// senderId a permitted participant of this sessionId at share time?"
// SessionParticipantService answers this by calling convo-backend's own
// internal API, not a local copy — see that class for why.
public record ParticipantDto(
        UUID userId,
        OffsetDateTime joinedAt
) {}