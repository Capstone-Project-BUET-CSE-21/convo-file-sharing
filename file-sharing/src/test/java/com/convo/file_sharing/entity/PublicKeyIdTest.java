package com.convo.file_sharing.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PublicKeyIdTest {

    @Test
    void equalUserIdAndAlgorithm_areEqual() {
        UUID userId = UUID.randomUUID();
        PublicKeyId a = new PublicKeyId(userId, "ECDSA-P256");
        PublicKeyId b = new PublicKeyId(userId, "ECDSA-P256");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void sameUserId_differentAlgorithm_areNotEqual() {
        UUID userId = UUID.randomUUID();
        PublicKeyId ecdsa = new PublicKeyId(userId, "ECDSA-P256");
        PublicKeyId ecdh = new PublicKeyId(userId, "ECDH-P256");
        assertNotEquals(ecdsa, ecdh);
    }
}

// Run this to test PublicKeyId.java : mvn test -Dtest=PublicKeyIdTest