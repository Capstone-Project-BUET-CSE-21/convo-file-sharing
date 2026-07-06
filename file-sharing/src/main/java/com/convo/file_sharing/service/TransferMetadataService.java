package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        String previousHash = findPreviousHash(request.sessionId());

        TransferMetadata entity = TransferMetadata.builder()
                .transferId(UUID.randomUUID())        // Task 2: server-generated
                .sessionId(request.sessionId())
                .senderId(request.senderId())
                .fileName(request.fileName())
                .fileSize(request.fileSize())
                .mimeType(request.mimeType())
                .previousHash(previousHash)            // Task 1
                .fileHash(null)                        // Task 3: filled in by PATCH later
                .signature(null)
                .timestamp(nowTruncatedForStorage()) // Task 2: server clock, not client
                .build();

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
        TransferMetadata entity = repository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("No pending transfer with id " + transferId));

        entity.setFileHash(patch.fileHash());
        entity.setSignature(patch.signature());

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

    private String findPreviousHash(UUID sessionId) {
        List<TransferMetadata> latest = repository.findBySessionIdOrderByTimestampDesc(
                sessionId, PageRequest.of(0, 1));

        if (latest.isEmpty()) {
            return null; // first transfer in the session
        }

        // Chain to the actual content hash of the transfer that preceded
        // this one. NOTE: this will legitimately be null if the previous
        // transfer hasn't been PATCHed with its fileHash yet (client started
        // a new transfer before finishing signing the last one) — confirm
        // with Suchi/Debashri whether their chain reconstruction needs to
        // handle that gap case specially, or whether the client should block
        // starting a new transfer until the prior one is finalized.
        return latest.get(0).getFileHash();
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