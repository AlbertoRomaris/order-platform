package com.orderplatform.worker.infrastructure.persistence.outbox;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.model.OutboxStatus;
import com.orderplatform.core.application.port.OutboxRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private final JdbcTemplate jdbc;

    public JdbcOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enqueue(OutboxEvent event) {
        // El worker no encola (eso lo hace la API). Lo dejamos explícito.
        throw new UnsupportedOperationException("Outbox enqueue is handled by the API service");
    }

    @Override
    public List<OutboxEvent> claimReady(int limit, Instant now, String lockedBy) {
        // 1) Selecciona eventos listos y los bloquea (evita que 2 workers cojan lo mismo)
        String sql = """
            SELECT id, aggregate_id, event_type, payload, status, attempts, next_attempt_at, created_at
            FROM order_outbox
            WHERE status = 'PENDING'
              AND next_attempt_at <= ?
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT ?
            """;

        return jdbc.query(sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(now));
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    @Override
    public void markProcessing(UUID eventId, Instant lockedAt, String lockedBy) {
        String sql = """
            UPDATE order_outbox
            SET status = 'PROCESSING',
                locked_at = ?,
                locked_by = ?
            WHERE id = ?
            """;

        jdbc.update(sql, Timestamp.from(lockedAt), lockedBy, eventId);
    }

    private OutboxEvent map(ResultSet rs) throws java.sql.SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID aggregateId = UUID.fromString(rs.getString("aggregate_id"));
        String eventType = rs.getString("event_type");
        String payload = rs.getString("payload");
        String statusStr = rs.getString("status");
        int attempts = rs.getInt("attempts");
        Instant nextAttemptAt = rs.getTimestamp("next_attempt_at").toInstant();
        Instant createdAt = rs.getTimestamp("created_at").toInstant();

        OutboxStatus status = OutboxStatus.valueOf(statusStr);

        return new OutboxEvent(
                id,
                aggregateId,
                eventType,
                payload,
                status,
                attempts,
                nextAttemptAt,
                createdAt
        );
    }

    @Override
    public void markProcessed(UUID eventId, Instant processedAt) {
        String sql = """
        UPDATE order_outbox
        SET status = 'PROCESSED',
            processed_at = ?,
            locked_at = NULL,
            locked_by = NULL
        WHERE id = ?
        """;
        jdbc.update(sql, Timestamp.from(processedAt), eventId);
    }

    @Override
    public void reschedule(UUID eventId, Instant nextAttemptAt, String lastError) {
        String sql = """
        UPDATE order_outbox
        SET status = 'PENDING',
            attempts = attempts + 1,
            next_attempt_at = ?,
            last_error = ?,
            locked_at = NULL,
            locked_by = NULL
        WHERE id = ?
        """;

        jdbc.update(sql, Timestamp.from(nextAttemptAt), lastError, eventId);
    }

    @Override
    public void markFailed(UUID eventId, Instant failedAt, String lastError) {
        String sql = """
        UPDATE order_outbox
        SET status = 'FAILED',
            processed_at = ?,
            last_error = ?,
            locked_at = NULL,
            locked_by = NULL
        WHERE id = ?
        """;

        jdbc.update(sql, Timestamp.from(failedAt), lastError, eventId);
    }


}
