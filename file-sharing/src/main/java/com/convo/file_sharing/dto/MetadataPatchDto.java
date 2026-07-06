package com.convo.file_sharing.dto;

import jakarta.validation.constraints.NotBlank;

// 3.1 Task 3: client posts these back once it has hashed/signed the file
// per sections 0.3/0.4. fileHash is hex, signature is base64 (section 0.4).
public record MetadataPatchDto(
        @NotBlank String fileHash,
        @NotBlank String signature
) {}
