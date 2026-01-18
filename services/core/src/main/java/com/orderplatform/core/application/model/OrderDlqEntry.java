package com.orderplatform.core.application.model;

import java.time.Instant;
import java.util.UUID;

public record OrderDlqEntry(
        UUID orderId,
        String reason,
        int retryCount,
        Instant failedAt
) {}
