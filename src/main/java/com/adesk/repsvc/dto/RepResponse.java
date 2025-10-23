package com.adesk.repsvc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RepResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        @JsonProperty("isActive") boolean isActive,
        Map<String, Object> attributes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
