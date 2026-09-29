package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.ChainHistoryResponseDto;
import com.convo.file_sharing.dto.MetadataPatchDto;
import com.convo.file_sharing.dto.MetadataRequestDto;
import com.convo.file_sharing.dto.MetadataResponseDto;
import com.convo.file_sharing.dto.RecipientDto;
import com.convo.file_sharing.entity.TransferMetadata;
import com.convo.file_sharing.entity.TransferRecipient;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.repository.TransferMetadataRepository;
import com.convo.file_sharing.repository.TransferRecipientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final TransferRecipientRepository recipientRepository;
    private final UserLookupClient userLookupClient;

    public TransferMetadataService(
            TransferMetadataRepository repository,
            TransferRecipientRepository recipientRepository,
            UserLookupClient userLookupClient) {
        this.repository = repository;
        this.recipientRepository = recipientRepository;
        this.userLookupClient = userLookupClient;
    }

    /**
     * Creates the pending row for one share. transferId and timestamp are
     * server-generated; fileHash/signature are filled in by the PATCH once
     * the client has hashed and signed.
     *
     * The server, not the client, decides which earlier share this one
     * continues: the most recent completed share of the same content that
     * named this sender as a recipient. If there is none, the sender never
     * received this content through Convo — they introduced it themselves
     * (the original sender sharing again, or a file that arrived by email,
     * USB, another app...) — and this share starts its own tree. So a file's
     * history is a forest: Charlie→Alice then Alice→Dave links Alice's share
     * under Charlie's, while Charlie sharing again later, or Bob sharing a
     * copy he got outside Convo, each starts a new root.
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

        TransferMetadata parent = recipientRepository
                .findSharesReceivedBy(request.senderId(), request.contentHash())
                .stream().findFirst().orElse(null);

        TransferMetadata entity = TransferMetadata.builder()
                .transferId(UUID.randomUUID())
                .sessionId(request.sessionId())
                .senderId(request.senderId())
                .fileName(request.fileName())
                .fileSize(request.fileSize())
                .mimeType(request.mimeType())
                .previousHash(parent == null ? null : parent.getFileHash())
                .originSessionId(parent == null ? request.sessionId() : parent.getOriginSessionId())
                .contentHash(request.contentHash())
                .fileHash(null)
                .signature(null)
                .timestamp(nowTruncatedForStorage())
                .build();

        Objects.requireNonNull(entity, "entity must not be null");
        TransferMetadata saved = repository.save(entity);

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
     * Completes a pending share once the client has signed the block
     * returned by createPendingTransfer. Only the original sender (per the
     * JWT, not the request body) may complete it, and the content hash must
     * be the one declared when the share was created — otherwise the share
     * would sit in the wrong file's history.
     */
    @Transactional
    public MetadataResponseDto attachHashAndSignature(UUID transferId, MetadataPatchDto patch, UUID authenticatedUserId) {
        Objects.requireNonNull(transferId, "transferId must not be null");

        TransferMetadata entity = repository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("No pending transfer with id " + transferId));

        if (!entity.getSenderId().equals(authenticatedUserId)) {
            throw new ForbiddenException("Only the original sender may complete this transfer");
        }
        if (!patch.contentHash().equals(entity.getContentHash())) {
            throw new IllegalArgumentException("contentHash does not match the one this transfer was created with");
        }

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

    /** Every completed share of this content, oldest first — all trees of the forest. */
    public List<ChainHistoryResponseDto> getChainHistory(String contentHash) {
        List<TransferMetadata> entries = repository.findByContentHashAndFileHashIsNotNullOrderByTimestampAsc(contentHash);

        // One batch query for every hop's recipients instead of N+1.
        List<UUID> transferIds = entries.stream().map(TransferMetadata::getTransferId).toList();
        Map<UUID, List<UUID>> recipientsByTransferId = transferIds.isEmpty()
                ? Collections.emptyMap()
                : recipientRepository.findByTransfer_TransferIdIn(transferIds).stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getTransfer().getTransferId(),
                                Collectors.mapping(TransferRecipient::getRecipientId, Collectors.toList())));

        // One batch call to convo-backend for every sender and recipient.
        Set<UUID> people = new LinkedHashSet<>();
        entries.forEach(e -> people.add(e.getSenderId()));
        recipientsByTransferId.values().forEach(people::addAll);
        Map<UUID, String> names = userLookupClient.getDisplayNames(List.copyOf(people));

        return entries.stream()
                .map(e -> new ChainHistoryResponseDto(
                        e.getTransferId(),
                        e.getSessionId(),
                        e.getOriginSessionId(),
                        e.getSenderId(),
                        names.get(e.getSenderId()),
                        e.getFileName(),
                        e.getFileSize(),
                        e.getMimeType(),
                        e.getTimestamp(),
                        e.getPreviousHash(),
                        e.getContentHash(),
                        e.getFileHash(),
                        e.getSignature(),
                        recipientsByTransferId.getOrDefault(e.getTransferId(), Collections.emptyList()).stream()
                                .map(id -> new RecipientDto(id, names.get(id)))
                                .toList()
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
