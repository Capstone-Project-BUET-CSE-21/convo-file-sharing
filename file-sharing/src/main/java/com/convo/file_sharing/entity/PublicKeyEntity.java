package com.convo.file_sharing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A public key a user has registered. Rows are additive, keyed by a surrogate
 * id — a user can hold several rows for the same algorithm over time as their
 * key rotates. The "current" key is the most recently created row; verification
 * tries every row so a signature made with a since-rotated key still verifies
 * (key history / rotation tolerance). This replaces the old composite
 * (user_id, algorithm) primary key, whose single-row-per-algorithm shape forced
 * registration to overwrite and stranded every prior signature on rotation.
 */
@Entity
@Table(name = "public_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "public_key", nullable = false, columnDefinition = "text")
    private String publicKey;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
