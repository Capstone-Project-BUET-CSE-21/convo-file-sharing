package com.convo.file_sharing.service;

import com.convo.file_sharing.config.InternalServiceProperties;
import com.convo.file_sharing.dto.ParticipantDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

// Backs the trace screen's isAuthorizedHop root-hop check (identity/
// traceVerification.js): "was this sender actually a participant of the
// session they claim to have first shared from?"
//
// There used to be a session_participants table here, written by an
// explicit client-side POST the moment a user joined a meeting — a
// duplicate of what convo-backend's meeting_user table already records
// from the same join event, kept in sync by nothing but the frontend
// remembering to call both endpoints. Per the team's design decision, no
// service holds a copy of another service's data or reaches into its
// tables directly (even where they happen to share a physical database) —
// so this asks convo-backend for it instead, the same way this service
// already asks convo-backend for display names (POST /api/backend/users/batch).
// No write path is needed here any more: convo-backend's own
// POST /api/backend/meeting-entry already creates the meeting_user row
// before a client ever reaches this service.
@Service
public class SessionParticipantService {

    private final RestClient restClient;
    private final InternalServiceProperties properties;

    public SessionParticipantService(InternalServiceProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBackendBaseUrl())
                .build();
    }

    // convo-backend's InternalMeetingController response shape.
    private record BackendParticipant(UUID userId, Instant joinedAt) {}

    public List<ParticipantDto> listParticipants(String sessionId) {
        try {
            BackendParticipant[] participants = restClient.get()
                    .uri("/api/backend/internal/meetings/{meetingCode}/participants", sessionId)
                    .header("X-Internal-Service-Key", properties.getServiceKey())
                    .retrieve()
                    .body(BackendParticipant[].class);

            if (participants == null) {
                return List.of();
            }
            return List.of(participants).stream()
                    .map(p -> new ParticipantDto(p.userId(), p.joinedAt().atOffset(ZoneOffset.UTC)))
                    .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                // Meeting code convo-backend has never heard of — no
                // participants, not an error (see isAuthorizedHop, which
                // treats "no participants" and "lookup failed" the same:
                // fail closed either way).
                return List.of();
            }
            throw e;
        }
    }
}
