package com.orderplatform.api.infrastructure.web.dto;

import com.orderplatform.api.infrastructure.persistence.OrderDlqEntity;

import java.time.Instant;
import java.util.UUID;

public record DlqEntryResponse(
        UUID orderId,
        String reason,
        int retryCount,
        Instant failedAt
) {
    public static DlqEntryResponse from(OrderDlqEntity e) {
        return new DlqEntryResponse(
                e.getOrderId(),
                e.getReason(),
                e.getRetryCount(),
                e.getFailedAt()
        );
    }
}
