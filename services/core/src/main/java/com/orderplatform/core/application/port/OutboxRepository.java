package com.orderplatform.core.application.port;

import com.orderplatform.core.application.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
    void enqueue(OutboxEvent event);
    List<OutboxEvent> claimReady(int limit, Instant now, String lockedBy);
    void markProcessing(UUID eventId, Instant lockedAt, String lockedBy);
    void markProcessed(UUID eventId, Instant processedAt);
    void reschedule(UUID eventId, Instant nextAttemptAt, String lastError);
    void markFailed(UUID eventId, Instant failedAt, String lastError);


}
