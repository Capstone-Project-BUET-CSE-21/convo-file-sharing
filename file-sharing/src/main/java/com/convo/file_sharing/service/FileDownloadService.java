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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileDownloadService {

    private final FileDownloadRepository repository;

    public FileDownloadService(FileDownloadRepository repository) {
        this.repository = repository;
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
        return toDto(saved);
    }

    public List<DownloadRecordDto> listDownloads(String contentHash) {
        return repository.findByContentHashOrderByDownloadedAtAsc(contentHash).stream()
                .map(this::toDto)
                .toList();
    }

    // Same reasoning as TransferMetadataService.nowTruncatedForStorage():
    // server clock (never client-supplied), truncated to the precision
    // Postgres actually stores, so the value returned from this call never
    // drifts from what a later read produces.
    private static OffsetDateTime nowTruncatedForStorage() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private DownloadRecordDto toDto(FileDownload d) {
        return new DownloadRecordDto(d.getId(), d.getSessionId(), d.getUserId(), d.getContentHash(), d.getDownloadedAt());
    }
}
