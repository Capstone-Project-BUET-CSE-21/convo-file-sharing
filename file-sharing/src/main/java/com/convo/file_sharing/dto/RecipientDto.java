package com.convo.file_sharing.dto;

import java.util.UUID;

// displayName is null when convo-backend couldn't resolve the id.
public record RecipientDto(UUID userId, String displayName) {}
