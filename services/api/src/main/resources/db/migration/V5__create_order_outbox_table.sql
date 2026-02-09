-- Outbox table for reliable event delivery (transactional outbox)
-- V2: first step towards separated worker + future SQS relay

CREATE TABLE IF NOT EXISTS order_outbox (
                                            id               UUID PRIMARY KEY,
                                            aggregate_id      UUID NOT NULL,                 -- orderId
                                            event_type        VARCHAR(100) NOT NULL,         -- e.g., OrderCreated
    payload           TEXT NULL,                     -- JSON/text (optional at first)
    status            VARCHAR(20) NOT NULL,          -- PENDING | PROCESSING | PROCESSED | FAILED
    attempts          INT NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    locked_at         TIMESTAMP WITH TIME ZONE NULL,
                                    locked_by         VARCHAR(100) NULL,
    processed_at      TIMESTAMP WITH TIME ZONE NULL,
    last_error        TEXT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );

-- Fast polling: find ready-to-process events
CREATE INDEX IF NOT EXISTS idx_outbox_ready
    ON order_outbox (status, next_attempt_at, created_at);

-- Optional: quickly find by aggregate for debugging
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON order_outbox (aggregate_id);
