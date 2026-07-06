package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record MetadataRequestDto(
        @NotNull UUID sessionId,
        @NotNull UUID senderId,
        @NotBlank String fileName,
        @Positive Long fileSize,
        @NotBlank String mimeType
) {}
