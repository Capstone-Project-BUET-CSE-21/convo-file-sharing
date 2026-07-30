package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.service.SessionParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// New endpoint — GET /api/sessions/{sessionId}/participants.
// This did not exist anywhere in convo-file-sharing before: Fariha built
// the session_participants table + repository for RLS/authorization
// purposes, but nothing read it back out over HTTP. Debashri's
// identity/traceVerification.js (frontend) calls this to resolve
// isAuthorizedHop for the trace/lineage screen.
//
// Flag to Fariha: confirm this is the right home for this route (vs. it
// living next to wherever session/meeting membership is otherwise
// exposed) before this ships.
@RestController
@RequestMapping("/api/sessions")
public class SessionParticipantController {

    private final SessionParticipantService service;

    public SessionParticipantController(SessionParticipantService service) {
        this.service = service;
    }

    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<ParticipantDto>> listParticipants(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(service.listParticipants(sessionId));
    }
}