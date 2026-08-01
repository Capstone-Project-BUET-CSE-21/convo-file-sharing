package com.convo.file_sharing.controller;

import com.convo.file_sharing.dto.KeyResponseDto;
import com.convo.file_sharing.exception.NotFoundException;
import com.convo.file_sharing.service.KeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;


@WebMvcTest(KeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class KeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeyService service;

    @Test
    void getByAlgorithm_returnsKey() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getKeyByAlgorithm(userId, "ECDH-P256"))
                .thenReturn(new KeyResponseDto("ecdh-dummy-value", "ECDH-P256"));

        mockMvc.perform(get("/api/keys/{userId}/{algorithm}", userId, "ECDH-P256"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("ecdh-dummy-value"))
                .andExpect(jsonPath("$.algorithm").value("ECDH-P256"));
    }

    @Test
    void getByAlgorithm_missingKey_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getKeyByAlgorithm(userId, "ECDH-P256"))
                .thenThrow(new NotFoundException("No ECDH-P256 public key registered for user " + userId));

        mockMvc.perform(get("/api/keys/{userId}/{algorithm}", userId, "ECDH-P256"))
                .andExpect(status().isNotFound());
    }
}