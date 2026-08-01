package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.TransferMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferMetadataRepository extends JpaRepository<TransferMetadata, UUID> {

    java.util.Optional<TransferMetadata> findByFileHash(String fileHash);

    List<TransferMetadata> findByContentHashOrderByTimestampAsc(String contentHash);
}
