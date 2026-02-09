package com.orderplatform.core.application.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
