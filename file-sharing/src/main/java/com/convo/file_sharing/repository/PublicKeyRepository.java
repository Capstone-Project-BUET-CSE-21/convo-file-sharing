package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.PublicKeyEntity;
import com.convo.file_sharing.entity.PublicKeyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicKeyRepository extends JpaRepository<PublicKeyEntity, PublicKeyId> {

    List<PublicKeyEntity> findByUserId(UUID userId);

    Optional<PublicKeyEntity> findByUserIdAndAlgorithm(UUID userId, String algorithm);
}