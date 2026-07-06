package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.KeyRegistrationDto;
import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.entity.PublicKeyEntity;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.PublicKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class KeyService {

    private final PublicKeyRepository repository;

    public KeyService(PublicKeyRepository repository) {
        this.repository = repository;
    }

    // 3.2 Task 1: register/replace a user's public key, backing Anisa's
    // keypair registration (2.2) — upsert on userId (primary key).
    @Transactional
    public void registerKey(KeyRegistrationDto dto) {
        PublicKeyEntity entity = PublicKeyEntity.builder()
                .userId(dto.userId())
                .publicKey(dto.publicKey())
                .algorithm(dto.algorithm())
                .createdAt(OffsetDateTime.now())
                .build();
        repository.save(entity);
    }

    // Backs Debashri's/Anisa's verification lookups (2.4).
    public KeyResponseDto getKey(UUID userId) {
        PublicKeyEntity entity = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("No public key registered for user " + userId));
        return new KeyResponseDto(entity.getPublicKey(), entity.getAlgorithm());
    }
}
