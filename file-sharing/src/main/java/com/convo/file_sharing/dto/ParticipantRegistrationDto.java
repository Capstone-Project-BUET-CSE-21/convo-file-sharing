package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Counterpart to the GET listing. Nothing writes to session_participants
// without this — without it, isAuthorizedHop can never return true, and
// screens/FileSharingTestPage.jsx's registerParticipant() call 404s.
public record ParticipantRegistrationDto(
        @NotNull UUID userId
) {}