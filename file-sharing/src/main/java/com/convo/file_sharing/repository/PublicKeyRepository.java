package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.PublicKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublicKeyRepository extends JpaRepository<PublicKeyEntity, UUID> {
    // findById(userId) and save(...) from JpaRepository already cover 3.2's CRUD needs.
}
