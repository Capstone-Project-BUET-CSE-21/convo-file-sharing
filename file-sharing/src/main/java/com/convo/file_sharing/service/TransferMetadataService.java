package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransferMetadataService {

    private final TransferMetadataRepository repository;

    public TransferMetadataService(TransferMetadataRepository repository) {
        this.repository = repository;
    }

    /**
     * 3.1 Task 1 + Task 2 + Task 3.
     * Looks up previousHash from the most recent row for the session,
     * generates transferId/timestamp server-side (never trust a client
     * timestamp — that's the whole point of Task 2), and persists a
     * pending row immediately with file_hash/signature left null, since
     * the client hasn't hashed or signed anything yet at this point.
     */
    @Transactional
    public MetadataResponseDto createPendingTransfer(MetadataRequestDto request) {
        String previousHash = request.previousHash();
        UUID originSessionId = request.sessionId();

        if (previousHash != null && !previousHash.trim().isEmpty()) {
            TransferMetadata prev = repository.findByFileHash(previousHash)
                    .orElseThrow(() -> new NotFoundException("Invalid previousHash: not found in durable chain store"));
            originSessionId = prev.getOriginSessionId();
        } else {
            previousHash = null;
        }

        TransferMetadata entity = TransferMetadata.builder()
                .transferId(UUID.randomUUID())        // Task 2: server-generated
                .sessionId(request.sessionId())
                .senderId(request.senderId())
                .fileName(request.fileName())
                .fileSize(request.fileSize())
                .mimeType(request.mimeType())
                .previousHash(previousHash)            // Task 1
                .originSessionId(originSessionId)      // Task 7
                .fileHash(null)                        // Task 3: filled in by PATCH later
                .contentHash(null)
                .signature(null)
                .timestamp(nowTruncatedForStorage())    // Task 2: server clock, not client
                .build();

        Objects.requireNonNull(entity, "entity must not be null");
        TransferMetadata saved = repository.save(entity);
        return toResponse(saved);
    }

    /**
     * 3.1 Task 3, follow-up call: PATCH /api/transfer/metadata/{transferId}.
     * The client has now built the signed block per sections 0.3/0.4 and
     * posts fileHash + signature back; we update the pending row rather
     * than creating a new one, so transfer_metadata stays the single
     * server-side audit record for this transfer (3.3 Task 3).
     */
    @Transactional
    public MetadataResponseDto attachHashAndSignature(UUID transferId, MetadataPatchDto patch) {
        Objects.requireNonNull(transferId, "transferId must not be null");

        TransferMetadata entity = repository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("No pending transfer with id " + transferId));

        if (entity.getPreviousHash() == null) {
            List<TransferMetadata> existing = repository.findByContentHashOrderByTimestampAsc(patch.contentHash());
            if (!existing.isEmpty()) {
                throw new IllegalArgumentException("Laundering gap detected: file content is known but no previousHash provided.");
            }
        }

        entity.setFileHash(patch.fileHash());
        entity.setSignature(patch.signature());
        entity.setContentHash(patch.contentHash());

        TransferMetadata saved = repository.save(entity);
        return toResponse(saved);
    }

    /**
     * Section 0.2 hashes/signs the literal timestamp STRING, not just the
     * instant it represents. Postgres timestamptz internally stores UTC at
     * microsecond precision — a plain OffsetDateTime.now() carries the JVM's
     * local zone offset and nanosecond precision, so the same instant would
     * print differently before persisting (e.g. "...010614616+06:00") vs.
     * after a DB round-trip (e.g. "...010615Z"). Truncating to UTC/micros
     * here, before it's ever handed to a client, means the value returned
     * from this POST is already exactly what any later read will produce —
     * the string a client canonicalizes/hashes against never drifts.
     */
    private static OffsetDateTime nowTruncatedForStorage() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    public List<com.convo.file_sharing.dto.ChainHistoryResponseDto> getChainHistory(String contentHash) {
        return repository.findByContentHashOrderByTimestampAsc(contentHash).stream()
                .map(e -> new com.convo.file_sharing.dto.ChainHistoryResponseDto(
                        e.getTransferId(),
                        e.getSessionId(),
                        e.getOriginSessionId(),
                        e.getSenderId(),
                        e.getFileName(),
                        e.getFileSize(),
                        e.getMimeType(),
                        e.getTimestamp(),
                        e.getPreviousHash(),
                        e.getContentHash()
                ))
                .toList();
    }

    private MetadataResponseDto toResponse(TransferMetadata e) {
        return new MetadataResponseDto(
                e.getTransferId(),
                e.getSessionId(),
                e.getSenderId(),
                e.getFileName(),
                e.getFileSize(),
                e.getMimeType(),
                e.getTimestamp(),
                e.getPreviousHash()
        );
    }
}