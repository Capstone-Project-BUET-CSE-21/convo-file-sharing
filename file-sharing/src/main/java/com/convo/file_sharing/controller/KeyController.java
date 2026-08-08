package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.KeyRegistrationDto;
import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.security.CurrentUser;
import com.convo.file_sharing.service.KeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/keys")
public class KeyController {

    private final KeyService service;

    public KeyController(KeyService service) {
        this.service = service;
    }

    // 3.2: POST /api/keys — a caller may only register a key for their own
    // authenticated identity (see KeyService.registerKey).
    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody KeyRegistrationDto dto) {
        service.registerKey(dto, CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3.2: GET /api/keys/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<KeyResponseDto> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getKey(userId));
    }

    // GET /api/keys/{userId}/{algorithm} — the user's CURRENT key for this
    // algorithm (most recently registered). Used where a single live key is
    // wanted, e.g. encrypting to a recipient's current ECDH key.
    @GetMapping("/{userId}/{algorithm}")
    public ResponseEntity<KeyResponseDto> getByAlgorithm(
            @PathVariable UUID userId,
            @PathVariable String algorithm) {
        return ResponseEntity.ok(service.getKeyByAlgorithm(userId, algorithm));
    }

    // GET /api/keys/{userId}/{algorithm}/all — every key the user has ever
    // registered for this algorithm (newest first). The receive-side verifier
    // tries each so a signature made with a since-rotated key still verifies.
    @GetMapping("/{userId}/{algorithm}/all")
    public ResponseEntity<List<KeyResponseDto>> getAllByAlgorithm(
            @PathVariable UUID userId,
            @PathVariable String algorithm) {
        return ResponseEntity.ok(service.getAllKeysByAlgorithm(userId, algorithm));
    }
}
