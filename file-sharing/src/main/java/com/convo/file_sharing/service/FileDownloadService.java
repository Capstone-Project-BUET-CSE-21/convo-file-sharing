package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.DownloadRecordDto;
import com.convo.file_sharing.dto.DownloadRegistrationDto;
import com.convo.file_sharing.entity.FileDownload;
import com.convo.file_sharing.exception.ForbiddenException;
import com.convo.file_sharing.repository.FileDownloadRepository;
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

// Same reasoning as TransferMetadataService's class-level suppression:
// Eclipse's null analysis flags FileDownload::getUserId (a Lombok-generated
// getter on a NOT NULL column) as needing an "unchecked conversion" when
// used as a method reference below — not a real null risk, just unannotated
// generated code the analyzer has no way to reason about.
@SuppressWarnings("null")
@Service
public class FileDownloadService {

    private final FileDownloadRepository repository;
    private final UserLookupClient userLookupClient;

    public FileDownloadService(FileDownloadRepository repository, UserLookupClient userLookupClient) {
        this.repository = repository;
        this.userLookupClient = userLookupClient;
    }

    /**
     * A user can only record their own download — userId must match the
     * JWT-derived authenticatedUserId, not an arbitrary value from the
     * request body. Same ownership rule as every other write in this
     * service; without it, anyone could log a download against a
     * different participant's identity.
     *
     * Every call inserts a new row rather than upserting one row per
     * (user, contentHash) — this is meant to be an append-only audit log
     * of download events (consistent with transfer_metadata rows never
     * being overwritten), not a one-time "has this user ever downloaded
     * it" flag, so re-downloads are recorded too.
     */
    @Transactional
    public DownloadRecordDto recordDownload(String sessionId, DownloadRegistrationDto request, UUID authenticatedUserId) {
        if (!request.userId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You can only record your own downloads");
        }

        FileDownload download = FileDownload.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .userId(request.userId())
                .contentHash(request.contentHash())
                .downloadedAt(nowTruncatedForStorage())
                .build();

        FileDownload saved = repository.save(Objects.requireNonNull(download));
        // A single-download response has nobody else's id to batch this
        // with — one-element lookup rather than skipping resolution here
        // and leaving the caller with a null name for their own action.
        Map<UUID, String> displayNames = userLookupClient.getDisplayNames(List.of(saved.getUserId()));
        return toDto(saved, displayNames);
    }

    public List<DownloadRecordDto> listDownloads(String contentHash) {
        List<FileDownload> downloads = repository.findByContentHashOrderByDownloadedAtAsc(contentHash);
        List<UUID> userIds = downloads.stream().map(FileDownload::getUserId).distinct().toList();
        Map<UUID, String> displayNames = userIds.isEmpty()
                ? Collections.emptyMap()
                : userLookupClient.getDisplayNames(userIds);
        return downloads.stream()
                .map(d -> toDto(d, displayNames))
                .toList();
    }

    // Same reasoning as TransferMetadataService.nowTruncatedForStorage():
    // server clock (never client-supplied), truncated to the precision
    // Postgres actually stores, so the value returned from this call never
    // drifts from what a later read produces.
    private static OffsetDateTime nowTruncatedForStorage() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private DownloadRecordDto toDto(FileDownload d, Map<UUID, String> displayNames) {
        return new DownloadRecordDto(
                d.getId(), d.getSessionId(), d.getUserId(), displayNames.get(d.getUserId()),
                d.getContentHash(), d.getDownloadedAt());
    }
}
