package com.adesk.repsvc.dto;

import java.time.OffsetDateTime;

public record RepEventPayload(
        String eventType,
        OffsetDateTime occurredAt,
        RepResponse rep
) {
}
