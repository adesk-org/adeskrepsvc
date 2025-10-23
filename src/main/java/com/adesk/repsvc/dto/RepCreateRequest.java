package com.adesk.repsvc.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record RepCreateRequest(
        @NotNull UUID userId,
        Boolean isActive,
        Map<String, Object> attributes
) {
}
