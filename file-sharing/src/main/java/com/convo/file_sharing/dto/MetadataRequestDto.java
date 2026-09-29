package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

// No previousHash: the server decides which earlier share this one continues
// (see TransferMetadataService.createPendingTransfer), so a client can't
// attach its share to a hop it never received. contentHash is what that
// decision is keyed on.
public record MetadataRequestDto(
        @NotBlank String sessionId,
        @NotNull UUID senderId,
        @NotBlank String fileName,
        @Positive Long fileSize,
        @NotBlank String mimeType,
        @NotBlank String contentHash,
        // Who the sender is handing this file to on this hop. A later share
        // by one of these recipients is attached to this hop.
        @NotEmpty List<UUID> recipients
) {}
