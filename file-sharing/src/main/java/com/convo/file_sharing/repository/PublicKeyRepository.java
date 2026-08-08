package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.PublicKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicKeyRepository extends JpaRepository<PublicKeyEntity, UUID> {

    // Current key for an algorithm = the most recently registered row.
    Optional<PublicKeyEntity> findFirstByUserIdAndAlgorithmOrderByCreatedAtDesc(UUID userId, String algorithm);

    // Full key history for an algorithm, newest first — the verification path
    // tries each so a signature from any of the user's past keys still verifies.
    List<PublicKeyEntity> findByUserIdAndAlgorithmOrderByCreatedAtDesc(UUID userId, String algorithm);

    // De-dupe guard: don't insert a row for a key this user already registered.
    boolean existsByUserIdAndAlgorithmAndPublicKey(UUID userId, String algorithm, String publicKey);
}
