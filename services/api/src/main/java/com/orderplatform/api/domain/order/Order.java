package com.orderplatform.api.domain.order;

import java.time.Instant;
import java.util.UUID;

public class Order {

    private final UUID id;
    private OrderStatus status;

    private final Instant createdAt;
    private Instant updatedAt;

    private int retryCount;
    private String failureReason; // nullable

    private Order(UUID id,
                  OrderStatus status,
                  Instant createdAt,
                  Instant updatedAt,
                  int retryCount,
                  String failureReason) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
    }

    public static Order newPending(UUID id, Instant now) {
        return new Order(id, OrderStatus.PENDING, now, now, 0, null);
    }

    /** Factory para reconstruir desde persistencia (DB). */
    public static Order restore(UUID id,
                                OrderStatus status,
                                Instant createdAt,
                                Instant updatedAt,
                                int retryCount,
                                String failureReason) {
        return new Order(id, status, createdAt, updatedAt, retryCount, failureReason);
    }

    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getRetryCount() { return retryCount; }
    public String getFailureReason() { return failureReason; }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    // State transitions explícitas

    public void markProcessing(Instant now) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order must be PENDING to start processing. Current=" + status);
        }
        status = OrderStatus.PROCESSING;
        failureReason = null;
        touch(now);
    }

    public void markProcessed(Instant now) {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order must be PROCESSING to be processed. Current=" + status);
        }
        status = OrderStatus.PROCESSED;
        touch(now);
    }

    public void markFailed(String reason, Instant now) {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order must be PROCESSING to fail. Current=" + status);
        }
        status = OrderStatus.FAILED;
        failureReason = (reason == null || reason.isBlank()) ? "unknown" : reason;
        retryCount += 1;
        touch(now);
    }

    public void markRetryableFailure(String reason, Instant now) {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Order must be PROCESSING to retry-fail. Current=" + status
            );
        }

        retryCount += 1;
        failureReason = (reason == null || reason.isBlank()) ? "unknown" : reason;

        // IMPORTANT: PENDING again
        status = OrderStatus.PENDING;
        touch(now);
    }

    public void resetToPending(String reason, Instant now) {
        // se permite resetear desde FAILED (típico reprocess)
        if (status != OrderStatus.FAILED) {
            throw new IllegalStateException("Only FAILED orders can be reprocessed. Current=" + status);
        }
        status = OrderStatus.PENDING;
        failureReason = reason;
        touch(now);
    }


}
