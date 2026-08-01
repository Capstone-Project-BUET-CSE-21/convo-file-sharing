package com.convo.file_sharing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transfer_metadata",
        indexes = @Index(
                name = "idx_transfer_metadata_content_hash_ts",
                columnList = "content_hash, timestamp"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMetadata {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    // Task 1: hash of/from the previous transfer in this session, null if first
    @Column(name = "previous_hash")
    private String previousHash;

    // Task 3: filled in later via PATCH once client posts fileHash/signature
    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "origin_session_id", length = 64)
    private String originSessionId;

    @Column(name = "signature")
    private String signature;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;
}
