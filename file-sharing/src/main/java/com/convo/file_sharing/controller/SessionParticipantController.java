package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.service.SessionParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionParticipantController {

    private final SessionParticipantService service;

    public SessionParticipantController(SessionParticipantService service) {
        this.service = service;
    }

    // No corresponding POST — convo-backend's own POST /api/backend/meeting-entry
    // is what creates the meeting_user row this reads (via SessionParticipantService),
    // before a client ever reaches this service. Registering presence here
    // separately would just be writing a second copy of that same event.
    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<ParticipantDto>> listParticipants(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.listParticipants(sessionId));
    }
}