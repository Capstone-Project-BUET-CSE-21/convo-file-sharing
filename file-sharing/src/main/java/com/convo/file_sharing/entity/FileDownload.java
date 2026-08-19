package com.convo.file_sharing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// Append-only audit log of download events: one row per time a participant
// downloads a shared file, keyed by contentHash (the same content-addressed
// hash transfer_metadata rows use) rather than a specific transferId/hop,
// since a download isn't tied to which hop the recipient happened to get
// the file from. Deliberately not upserted into one row per (user,
// contentHash) — re-downloads are meaningful audit events on their own,
// mirroring transfer_metadata's own append-only history.
@Entity
@Table(
        name = "file_downloads",
        indexes = @Index(
                name = "idx_file_downloads_content_hash_ts",
                columnList = "content_hash, downloaded_at"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDownload {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "downloaded_at", nullable = false)
    private OffsetDateTime downloadedAt;
}
