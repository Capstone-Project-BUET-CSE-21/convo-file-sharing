package com.convo.file_sharing.service;

import com.convo.file_sharing.dto.KeyRegistrationDto;
import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KeyServiceTest {

    @Autowired
    private KeyService keyService;

    @Test
    void getKey_returnsEcdsaKey_backwardCompatible() {
        UUID userId = UUID.randomUUID();
        keyService.registerKey(new KeyRegistrationDto(userId, "ecdsa-value", "ECDSA-P256"), userId);
        keyService.registerKey(new KeyRegistrationDto(userId, "ecdh-value", "ECDH-P256"), userId);

        KeyResponseDto result = keyService.getKey(userId);

        assertEquals("ecdsa-value", result.publicKey());
        assertEquals("ECDSA-P256", result.algorithm());
    }

    @Test
    void getKeyByAlgorithm_returnsEcdhKey() {
        UUID userId = UUID.randomUUID();
        keyService.registerKey(new KeyRegistrationDto(userId, "ecdsa-value", "ECDSA-P256"), userId);
        keyService.registerKey(new KeyRegistrationDto(userId, "ecdh-value", "ECDH-P256"), userId);

        KeyResponseDto result = keyService.getKeyByAlgorithm(userId, "ECDH-P256");

        assertEquals("ecdh-value", result.publicKey());
    }

    @Test
    void getKeyByAlgorithm_missingAlgorithm_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        keyService.registerKey(new KeyRegistrationDto(userId, "ecdsa-value", "ECDSA-P256"), userId);

        assertThrows(NotFoundException.class, () -> keyService.getKeyByAlgorithm(userId, "ECDH-P256"));
    }
}