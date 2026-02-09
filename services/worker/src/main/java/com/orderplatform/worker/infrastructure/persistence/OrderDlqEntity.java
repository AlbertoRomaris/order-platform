package com.orderplatform.worker.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_dlq")
public class OrderDlqEntity {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    protected OrderDlqEntity() {}

    public OrderDlqEntity(UUID orderId, String reason, int retryCount, Instant failedAt) {
        this.orderId = orderId;
        this.reason = reason;
        this.retryCount = retryCount;
        this.failedAt = failedAt;
    }

    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public int getRetryCount() { return retryCount; }
    public Instant getFailedAt() { return failedAt; }
}
