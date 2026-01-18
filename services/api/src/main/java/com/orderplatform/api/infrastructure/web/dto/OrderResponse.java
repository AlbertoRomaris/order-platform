package com.orderplatform.api.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String status,
        int retryCount,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
