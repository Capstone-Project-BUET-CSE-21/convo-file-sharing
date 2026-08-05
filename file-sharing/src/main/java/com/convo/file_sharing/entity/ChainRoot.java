package com.convo.file_sharing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One row per content hash that has ever claimed the root (previousHash ==
 * null) of a provenance chain. contentHash is the primary key, so the
 * database itself — not a check-then-act SELECT — is what makes "only one
 * root per content hash" hold under concurrent PATCH /api/transfer/metadata
 * requests. See TransferMetadataService.claimChainRoot.
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

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;
}
