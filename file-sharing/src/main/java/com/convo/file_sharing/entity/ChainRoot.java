package com.convo.file_sharing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per content hash that has ever claimed the root (previousHash ==
 * null) of a provenance chain. contentHash is the primary key, so the
 * database itself — not a check-then-act SELECT — is what makes "only one
 * root per content hash" hold under concurrent PATCH /api/file-sharing/transfer/metadata
 * requests. See TransferMetadataService.claimChainRoot.
 *
 * transfer is a real foreign key (transfer_id -> transfer_metadata.transfer_id)
 * — both entities live in this service, so unlike references to
 * convo-backend's users this one is a same-service relationship the
 * database can actually enforce.
 */
@Entity
@Table(name = "chain_roots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainRoot {

    @Id
    @Column(name = "content_hash")
    private String contentHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private TransferMetadata transfer;
}
