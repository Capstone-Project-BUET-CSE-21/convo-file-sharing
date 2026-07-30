package com.convo.file_sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// New — nothing in the repo currently exposes session_participants over
// HTTP. Debashri's isAuthorizedHop (identity/traceVerification.js) needs
// this to answer "was this senderId a permitted participant of this
// sessionId at share time?" Fariha's SessionParticipant entity/repository
// already exist (repository/SessionParticipantRepository.java), this DTO
// + the controller/service below just surface them.
public record ParticipantDto(
        UUID userId,
        OffsetDateTime joinedAt
) {}