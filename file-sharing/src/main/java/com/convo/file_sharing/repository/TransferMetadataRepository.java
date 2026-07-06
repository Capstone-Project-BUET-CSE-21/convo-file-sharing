package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.TransferMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferMetadataRepository extends JpaRepository<TransferMetadata, UUID> {

    // 3.1 Task 1 + 3.3 Task 2: most recent row for a session, to populate
    // previousHash. Backed by idx_transfer_metadata_session_ts
    // (session_id, timestamp desc) — Pageable(0,1) turns this into a
    // "LIMIT 1" the planner can satisfy directly from the index.
    List<TransferMetadata> findBySessionIdOrderByTimestampDesc(UUID sessionId, Pageable pageable);
}
