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
@Table(name = "transfer_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMetadata {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

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

    @Column(name = "signature")
    private String signature;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;
}
