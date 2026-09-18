package com.convo.file_sharing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// One row per (transfer, intended recipient) pair, written once alongside
// the pending TransferMetadata row and never mutated afterwards. This is
// what makeIsAuthorizedHop (frontend identity/traceVerification.js) checks
// a forwarded hop's sender against, instead of session_participants —
// "was this sender someone the *previous holder* actually sent this file
// to," not just "was this sender in the same meeting at some point."
//
// transfer is a real foreign key (transfer_id -> transfer_metadata.transfer_id)
// — same-service reference, enforced at the database level. recipientId
// stays a plain column: it points at convo-backend's users, a different
// service, which this service deliberately never holds a real FK into.
@Entity
@Table(
        name = "transfer_recipients",
        indexes = @Index(name = "idx_transfer_recipients_transfer_id", columnList = "transfer_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private TransferMetadata transfer;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;
}
