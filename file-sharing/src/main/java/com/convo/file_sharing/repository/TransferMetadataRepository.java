package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.TransferMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferMetadataRepository extends JpaRepository<TransferMetadata, UUID> {

    // Completed shares only (fileHash is set by the PATCH once the client has
    // signed). A pending row whose sender never finished is not history.
    List<TransferMetadata> findByContentHashAndFileHashIsNotNullOrderByTimestampAsc(String contentHash);
}
