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
@Table(name = "public_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicKeyEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "public_key", nullable = false, columnDefinition = "text")
    private String publicKey;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
