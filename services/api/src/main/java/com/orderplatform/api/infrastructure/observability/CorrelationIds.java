package com.orderplatform.api.infrastructure.observability;

import org.slf4j.MDC;

import java.util.Optional;

public final class CorrelationIds {
    private CorrelationIds() {}

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get("correlationId")).filter(s -> !s.isBlank());
    }
}
