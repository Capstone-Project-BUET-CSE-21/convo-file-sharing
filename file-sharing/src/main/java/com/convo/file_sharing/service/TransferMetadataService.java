package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.entity.ChainRoot;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.entity.TransferRecipient;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.ChainRootRepository;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import com.convo.file_sharing.repository.TransferRecipientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// Eclipse's null analysis (java.compile.nullAnalysis.mode=automatic) flags
// essentially every Lombok-generated entity getter and Spring Data
// repository call in this class as an "unchecked conversion" — none of it
// reflects a real null risk. TransferMetadata/TransferRecipient's fields
// are backed by NOT NULL database columns; JPA never returns a loaded
// entity with a null value in one. The warnings are just Eclipse having no
// way to know that (nothing here is annotated for nullability), not a sign
// anything needs a runtime check. Suppressed at the class level rather
// than scattered per-expression, since that's genuinely the honest scope
// of the "problem."
@SuppressWarnings("null")
@Service
public class TransferMetadataService {

    private final TransferMetadataRepository repository;
    private final ChainRootRepository chainRootRepository;
    private final TransferRecipientRepository recipientRepository;

    public TransferMetadataService(
            TransferMetadataRepository repository,
            ChainRootRepository chainRootRepository,
            TransferRecipientRepository recipientRepository) {
        this.repository = repository;
        this.chainRootRepository = chainRootRepository;
        this.recipientRepository = recipientRepository;
    }

    /**
     * 3.1 Task 1 + Task 2 + Task 3.
     * Looks up previousHash from the most recent row for the session,
     * generates transferId/timestamp server-side (never trust a client
     * timestamp — that's the whole point of Task 2), and persists a
     * pending row immediately with file_hash/signature left null, since
     * the client hasn't hashed or signed anything yet at this point.
     *
     * authenticatedUserId comes from the caller's JWT (CurrentUser) — the
     * request body's senderId must match it, otherwise anyone could create
     * provenance history under someone else's identity.
     */
    @Transactional
    public MetadataResponseDto createPendingTransfer(MetadataRequestDto request, UUID authenticatedUserId) {
        if (!request.senderId().equals(authenticatedUserId)) {
            throw new ForbiddenException("senderId must match the authenticated user");
        }

        String previousHash = request.previousHash();
        String originSessionId = request.sessionId();

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

        // Record who this hop's sender is actually handing the file to.
        // Written once, immutable afterwards — this is the ACL a later
        // hop's authorization check is measured against (see
        // makeIsAuthorizedHop in the frontend), not session attendance.
        List<UUID> recipientIds = List.copyOf(request.recipients());
        List<TransferRecipient> recipientRows = recipientIds.stream()
                .map(recipientId -> TransferRecipient.builder()
                        .transfer(saved)
                        .recipientId(recipientId)
                        .build())
                .toList();
        recipientRepository.saveAll(recipientRows);

        return toResponse(saved, recipientIds);
    }

    /**
     * 3.1 Task 3, follow-up call: PATCH /api/transfer/metadata/{transferId}.
     * The client has now built the signed block per sections 0.3/0.4 and
     * posts fileHash + signature back; we update the pending row rather
     * than creating a new one, so transfer_metadata stays the single
     * server-side audit record for this transfer (3.3 Task 3).
     *
     * Only the sender who created the pending row (per the JWT, not the
     * request body) may complete it — otherwise anyone who learned a
     * transferId could attach an arbitrary hash/signature to someone
     * else's transfer.
     */
    @Transactional
    public MetadataResponseDto attachHashAndSignature(UUID transferId, MetadataPatchDto patch, UUID authenticatedUserId) {
        Objects.requireNonNull(transferId, "transferId must not be null");

        TransferMetadata entity = repository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("No pending transfer with id " + transferId));

        if (!entity.getSenderId().equals(authenticatedUserId)) {
            throw new ForbiddenException("Only the original sender may complete this transfer");
        }

        if (entity.getPreviousHash() == null) {
            claimChainRoot(patch.contentHash(), entity);
        }

        entity.setFileHash(patch.fileHash());
        entity.setSignature(patch.signature());
        entity.setContentHash(patch.contentHash());

        TransferMetadata saved = repository.save(entity);
        return toResponse(saved);
    }

    /**
     * Atomically claims "this content hash's chain starts here." Replaces
     * the old find-then-check ("laundering gap") logic, which raced: two
     * concurrent first-shares of the same fresh file could both query an
     * empty history and both win, producing two unlinked root entries for
     * the same content. content_hash is chain_roots' primary key, so the
     * database — not a SELECT this code runs first — is what enforces
     * uniqueness; a losing concurrent writer gets a constraint violation
     * on save, not a successful insert. This also subsumes the original
     * laundering check: content that was ever shared before already holds
     * this row, so a later attempt to claim it as a fresh root (malicious
     * or not) hits the same conflict.
     */
    private void claimChainRoot(String contentHash, TransferMetadata transfer) {
        ChainRoot root = ChainRoot.builder().contentHash(contentHash).transfer(transfer).build();
        try {
            chainRootRepository.saveAndFlush(Objects.requireNonNull(root));
        } catch (DataIntegrityViolationException conflict) {
            throw new IllegalArgumentException(
                    "This file content already has a chain root from another transfer — "
                            + "refetch its history and link via previousHash instead of starting a new chain.");
        }
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
        List<TransferMetadata> entries = repository.findByContentHashOrderByTimestampAsc(contentHash);

        // One batch query for every hop's recipients instead of N+1 — chains
        // are short in practice, but there's no reason to pay per-hop
        // round trips for something known up front.
        List<UUID> transferIds = entries.stream().map(TransferMetadata::getTransferId).toList();
        Map<UUID, List<UUID>> recipientsByTransferId = transferIds.isEmpty()
                ? Collections.emptyMap()
                : recipientRepository.findByTransfer_TransferIdIn(transferIds).stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getTransfer().getTransferId(),
                                Collectors.mapping(TransferRecipient::getRecipientId, Collectors.toList())));

        return entries.stream()
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
                        e.getContentHash(),
                        e.getFileHash(),
                        e.getSignature(),
                        recipientsByTransferId.getOrDefault(e.getTransferId(), Collections.emptyList())
                ))
                .toList();
    }

    private MetadataResponseDto toResponse(TransferMetadata e) {
        List<UUID> recipients = recipientRepository.findByTransfer_TransferId(e.getTransferId()).stream()
                .map(TransferRecipient::getRecipientId)
                .toList();
        return toResponse(e, recipients);
    }

    private MetadataResponseDto toResponse(TransferMetadata e, List<UUID> recipients) {
        return new MetadataResponseDto(
                e.getTransferId(),
                e.getSessionId(),
                e.getSenderId(),
                e.getFileName(),
                e.getFileSize(),
                e.getMimeType(),
                e.getTimestamp(),
                e.getPreviousHash(),
                recipients
        );
    }
}