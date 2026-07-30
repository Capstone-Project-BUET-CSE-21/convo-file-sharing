package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.entity.SessionParticipant;
import com.convo.file_sharing.repository.SessionParticipantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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
    // Debashri's plan explicitly flags that "meeting attendance" and
    // "authorized to hold this specific file" may need to diverge into a
    // separate table down the line; this method only answers the former
    // for now.
    public List<ParticipantDto> listParticipants(UUID sessionId) {
        return repository.findBySessionId(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    private ParticipantDto toDto(SessionParticipant p) {
        return new ParticipantDto(p.getUserId(), p.getJoinedAt());
    }
}