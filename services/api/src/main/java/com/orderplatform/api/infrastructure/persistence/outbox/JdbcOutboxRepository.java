package com.orderplatform.api.infrastructure.persistence.outbox;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OutboxRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private final JdbcTemplate jdbc;

    private static final Logger log = LoggerFactory.getLogger(JdbcOutboxRepository.class);

    public JdbcOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enqueue(OutboxEvent event) {

        String sql = """
            INSERT INTO order_outbox
              (id, aggregate_id, event_type, payload, status, attempts, next_attempt_at, created_at)
            VALUES
              (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbc.update(
                sql,
                event.id(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                event.status().name(),
                event.attempts(),
                Timestamp.from(event.nextAttemptAt()),
                Timestamp.from(event.createdAt())
        );
    }

    // En API no se hace polling
    @Override
    public List<OutboxEvent> claimReady(int limit, Instant now, String workerId) {
        throw new UnsupportedOperationException("Outbox polling is handled by worker");
    }

    @Override
    public void markProcessing(UUID eventId, Instant lockedAt, String workerId) {
        throw new UnsupportedOperationException("Outbox polling is handled by worker");
    }

    @Override
    public void markProcessed(UUID eventId, Instant processedAt) {
        throw new UnsupportedOperationException("Outbox polling is handled by worker");
    }

    @Override
    public void reschedule(UUID eventId, Instant nextAttemptAt, String lastError) {
        throw new UnsupportedOperationException("Outbox polling is handled by worker");
    }

    @Override
    public void markFailed(UUID eventId, Instant failedAt, String lastError) {
        throw new UnsupportedOperationException("Outbox polling is handled by worker");
    }
}
