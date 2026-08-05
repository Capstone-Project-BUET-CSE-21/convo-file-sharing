package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.KeyRegistrationDto;
import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.entity.PublicKeyEntity;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.PublicKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class KeyService {

    private final PublicKeyRepository repository;

    public KeyService(PublicKeyRepository repository) {
        this.repository = repository;
    }

    // 3.2 Task 1: register/replace a user's public key, backing Anisa's
    // keypair registration (2.2) — upsert on userId (primary key).
    // authenticatedUserId comes from the caller's JWT (CurrentUser), never
    // the request body — without this check, anyone could register (or
    // silently overwrite) a signing/encryption key for someone else's id.
    @Transactional
    public void registerKey(KeyRegistrationDto dto, UUID authenticatedUserId) {
        if (!dto.userId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You can only register a key for your own account");
        }

        PublicKeyEntity entity = PublicKeyEntity.builder()
                .userId(dto.userId())
                .publicKey(dto.publicKey())
                .algorithm(dto.algorithm())
                .createdAt(OffsetDateTime.now())
                .build();

        Objects.requireNonNull(entity, "entity must not be null");
        repository.save(entity);
    }

    // Preserves original behavior: returns the ECDSA signing key for a user.
    public KeyResponseDto getKey(UUID userId) {
        return getKeyByAlgorithm(userId, "ECDSA-P256");
    }

    // New: fetch a specific algorithm's key for a user (e.g. ECDH-P256 for encryption).
    public KeyResponseDto getKeyByAlgorithm(UUID userId, String algorithm) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        PublicKeyEntity entity = repository.findByUserIdAndAlgorithm(userId, algorithm)
                .orElseThrow(() -> new NotFoundException(
                        "No " + algorithm + " public key registered for user " + userId));
        return new KeyResponseDto(entity.getPublicKey(), entity.getAlgorithm());
    }
}
