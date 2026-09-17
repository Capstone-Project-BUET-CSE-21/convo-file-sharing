package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record MetadataRequestDto(
        @NotBlank String sessionId,
        @NotNull UUID senderId,
        @NotBlank String fileName,
        @Positive Long fileSize,
        @NotBlank String mimeType,
        String previousHash,
        // Who the sender is actually handing this file to on this hop. This
        // is what per-file authorization checks against on every later hop
        // (see TransferRecipient / makeIsAuthorizedHop) — NOT part of the
        // section 0 signed provenance block, just server-side ACL metadata.
        @NotEmpty List<UUID> recipients
) {}
