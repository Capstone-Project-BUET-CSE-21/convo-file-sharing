package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.dto.ParticipantRegistrationDto;
import com.convo.file_sharing.security.CurrentUser;
import com.convo.file_sharing.service.SessionParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<ParticipantDto>> listParticipants(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.listParticipants(sessionId));
    }

    // A caller may only register their own presence in a session — letting
    // dto.userId() be anyone else would make the trace screen's
    // isAuthorizedHop check trivially satisfiable by an attacker adding
    // themselves (or a victim) as a participant of any session.
    @PostMapping("/{sessionId}/participants")
    public ResponseEntity<ParticipantDto> addParticipant(
            @PathVariable String sessionId,
            @Valid @RequestBody ParticipantRegistrationDto dto) {
        ParticipantDto saved = service.addParticipant(sessionId, dto.userId(), CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}