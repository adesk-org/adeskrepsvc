package com.adesk.repsvc.dto;

import java.util.List;

public record RepPageResponse(
        List<RepResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
