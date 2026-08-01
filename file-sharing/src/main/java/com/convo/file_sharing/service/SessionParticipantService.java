package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.entity.SessionParticipant;
import com.convo.file_sharing.repository.SessionParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SessionParticipantService {

    private final SessionParticipantRepository repository;

    public SessionParticipantService(SessionParticipantRepository repository) {
        this.repository = repository;
    }

    // Backs Debashri's authorized/unauthorized-per-hop check (5.3 / trace
    // screen). NOTE: session_participants is a point-in-time snapshot of who
    // was authorized when the file was shared (per the plan's schema
    // decision) — it deliberately does NOT reflect later-revoked access.
    public List<ParticipantDto> listParticipants(String sessionId) {
        return repository.findBySessionId(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    // Required by screens/FileSharingTestPage.jsx's registerParticipant()
    // call. Without this, session_participants can never actually be
    // written to, so isAuthorizedHop can never return true and the
    // "authorized" branch of the trace screen is untestable.
    @Transactional
    public ParticipantDto addParticipant(String sessionId, UUID userId) {
        SessionParticipant participant = SessionParticipant.builder()
                .sessionId(sessionId)
                .userId(userId)
                .joinedAt(OffsetDateTime.now())
                .build();
        SessionParticipant saved = repository.save(Objects.requireNonNull(participant));
        return toDto(saved);
    }

    private ParticipantDto toDto(SessionParticipant p) {
        return new ParticipantDto(p.getUserId(), p.getJoinedAt());
    }
}