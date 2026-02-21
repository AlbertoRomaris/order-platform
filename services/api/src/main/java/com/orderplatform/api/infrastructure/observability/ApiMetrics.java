package com.orderplatform.api.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ApiMetrics {

    private final MeterRegistry registry;

    private final Counter ordersCreatedTotal;
    private final Timer createOrderTimer;

    public ApiMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.ordersCreatedTotal = Counter.builder("order_api_orders_created_total")
                .description("Total number of orders created successfully by the API")
                .register(registry);

        this.createOrderTimer = Timer.builder("order_api_create_order_seconds")
                .description("Time spent in the create-order use case (transaction + persistence + event publish)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incOrdersCreated() {
        ordersCreatedTotal.increment();
    }

    public <T> T recordCreateOrder(java.util.concurrent.Callable<T> callable) throws Exception {
        return createOrderTimer.recordCallable(callable);
    }

    public void incEventPublished(String transport) {
        registry.counter(
                "order_api_events_published_total",
                "transport", normalizeTransport(transport)
        ).increment();
    }

    public void incEventPublishFailed(String transport) {
        registry.counter(
                "order_api_event_publish_failed_total",
                "transport", normalizeTransport(transport)
        ).increment();
    }

    private String normalizeTransport(String transport) {
        if (transport == null) return "unknown";
        String t = transport.trim().toLowerCase();
        return switch (t) {
            case "sqs", "outbox" -> t;
            default -> "unknown";
        };
    }
}