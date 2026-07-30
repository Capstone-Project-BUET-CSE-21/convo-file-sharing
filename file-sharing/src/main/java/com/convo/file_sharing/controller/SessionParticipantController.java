package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.ParticipantDto;
import com.convo.file_sharing.dto.ParticipantRegistrationDto;
import com.convo.file_sharing.service.SessionParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    // Required by screens/FileSharingTestPage.jsx's registerParticipant()
    // call — was missing from the last commit, causing that call to 404.
    @PostMapping("/{sessionId}/participants")
    public ResponseEntity<ParticipantDto> addParticipant(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ParticipantRegistrationDto dto) {
        ParticipantDto saved = service.addParticipant(sessionId, dto.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}