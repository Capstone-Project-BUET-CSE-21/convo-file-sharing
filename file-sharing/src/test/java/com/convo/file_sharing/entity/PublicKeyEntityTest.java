package com.convo.file_sharing.entity;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
// mvn test -Dtest=PublicKeyEntityTest 
class PublicKeyEntityTest {

    @Test
    void entityBuilds_withUserIdAndAlgorithm() {
        UUID userId = UUID.randomUUID();
        PublicKeyEntity entity = PublicKeyEntity.builder()
                .userId(userId)
                .algorithm("ECDH-P256")
                .publicKey("dummy-base64-key")
                .createdAt(OffsetDateTime.now())
                .build();

        assertEquals(userId, entity.getUserId());
        assertEquals("ECDH-P256", entity.getAlgorithm());
    }
}