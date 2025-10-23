package com.adesk.repsvc.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record RepPutRequest(
        @NotNull UUID userId,
        @NotNull Boolean isActive,
        @NotNull Map<String, Object> attributes
) {
}
