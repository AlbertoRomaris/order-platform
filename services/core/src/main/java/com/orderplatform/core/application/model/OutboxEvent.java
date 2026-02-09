package com.orderplatform.core.application.model;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant createdAt
) {
    public static OutboxEvent pending(UUID aggregateId, String eventType, String payload, Instant now) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                0,
                now,
                now
        );
    }
}
