package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KeyRegistrationDto(
        @NotNull UUID userId,
        @NotBlank String publicKey,
        @NotBlank String algorithm
) {}
