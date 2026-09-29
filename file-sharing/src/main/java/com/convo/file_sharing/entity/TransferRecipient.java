package com.convo.file_sharing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// One row per (transfer, intended recipient) pair, written once alongside
// the pending TransferMetadata row and never mutated afterwards. Informational
// record of who a transfer was addressed to — echoed back in
// MetadataResponseDto/ChainHistoryResponseDto. Not used to gate anything:
// there used to be a per-file authorization check measured against this
// (frontend's makeIsAuthorizedHop), removed deliberately — see
// convo-frontend's identity/traceVerification.js for why (it could only
// confirm sharing that happened through Convo itself, so legitimate sharing
// through any other channel looked identical to an actual leak).
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
