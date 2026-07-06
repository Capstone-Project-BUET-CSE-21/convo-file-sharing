package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.KeyRegistrationDto;
import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.service.KeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/keys")
public class KeyController {

    private final KeyService service;

    public KeyController(KeyService service) {
        this.service = service;
    }

    // 3.2: POST /api/keys
    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody KeyRegistrationDto dto) {
        service.registerKey(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3.2: GET /api/keys/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<KeyResponseDto> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getKey(userId));
    }
}
