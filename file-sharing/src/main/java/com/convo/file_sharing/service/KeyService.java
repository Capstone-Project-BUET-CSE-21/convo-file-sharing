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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class KeyService {

    private final PublicKeyRepository repository;

    public KeyService(PublicKeyRepository repository) {
        this.repository = repository;
    }

    // 3.2 Task 1: register a user's public key. Additive, NOT an upsert — every
    // key a user has ever registered is kept, so a signature made with an older
    // key stays verifiable after a rotation (see getAllKeysByAlgorithm). The old
    // overwrite behavior is exactly what stranded prior signatures when a key
    // changed (e.g. a fresh browser origin regenerating a keypair).
    // authenticatedUserId comes from the caller's JWT (CurrentUser), never the
    // request body, so a caller can only register a key for their own identity.
    @Transactional
    public void registerKey(KeyRegistrationDto dto, UUID authenticatedUserId) {
        if (!dto.userId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You can only register a key for your own account");
        }

        // Re-registering the exact same key (e.g. an idempotent retry, or the
        // same key across logins) shouldn't pile up duplicate rows.
        if (repository.existsByUserIdAndAlgorithmAndPublicKey(dto.userId(), dto.algorithm(), dto.publicKey())) {
            return;
        }

        PublicKeyEntity entity = PublicKeyEntity.builder()
                .userId(dto.userId())
                .publicKey(dto.publicKey())
                .algorithm(dto.algorithm())
                .createdAt(OffsetDateTime.now())
                .build();

        repository.save(Objects.requireNonNull(entity));
    }

    // Preserves original behavior: returns the ECDSA signing key for a user.
    public KeyResponseDto getKey(UUID userId) {
        return getKeyByAlgorithm(userId, "ECDSA-P256");
    }

    // The "current" key for an algorithm = the most recently registered one.
    // Used where a single live key is wanted (e.g. encrypting to a recipient's
    // current ECDH key).
    public KeyResponseDto getKeyByAlgorithm(UUID userId, String algorithm) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        PublicKeyEntity entity = repository.findFirstByUserIdAndAlgorithmOrderByCreatedAtDesc(userId, algorithm)
                .orElseThrow(() -> new NotFoundException(
                        "No " + algorithm + " public key registered for user " + userId));
        return new KeyResponseDto(entity.getPublicKey(), entity.getAlgorithm());
    }

    // Every key a user has registered for an algorithm, newest first. The
    // receive-side verifier tries each so a signature from any of the user's
    // past keys still verifies — this is what makes key rotation non-destructive.
    public List<KeyResponseDto> getAllKeysByAlgorithm(UUID userId, String algorithm) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return repository.findByUserIdAndAlgorithmOrderByCreatedAtDesc(userId, algorithm).stream()
                .map(e -> new KeyResponseDto(e.getPublicKey(), e.getAlgorithm()))
                .toList();
    }
}
