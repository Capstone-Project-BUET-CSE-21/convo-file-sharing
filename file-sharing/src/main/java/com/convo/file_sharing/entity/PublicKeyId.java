package com.convo.file_sharing.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PublicKeyId implements Serializable {

    private UUID userId;
    private String algorithm;

    public PublicKeyId() {
    }

    public PublicKeyId(UUID userId, String algorithm) {
        this.userId = userId;
        this.algorithm = algorithm;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyId)) return false;
        PublicKeyId that = (PublicKeyId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(algorithm, that.algorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, algorithm);
    }
}