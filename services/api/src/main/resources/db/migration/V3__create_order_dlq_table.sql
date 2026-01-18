CREATE TABLE order_dlq (
                           id UUID PRIMARY KEY,
                           order_id UUID NOT NULL,
                           reason TEXT NOT NULL,
                           retry_count INT NOT NULL,
                           failed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_dlq_order_id ON order_dlq(order_id);
