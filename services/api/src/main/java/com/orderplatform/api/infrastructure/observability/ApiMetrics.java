package com.orderplatform.api.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ApiMetrics {

    private final MeterRegistry registry;

    public final Counter ordersCreatedTotal;
    public final Timer createOrderTimer;

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

    public void eventPublished(String transport) {
        Counter.builder("order_api_events_published_total")
                .description("Total number of order events published successfully by the API")
                .tag("transport", transport)
                .register(registry)
                .increment();
    }

    public void eventPublishFailed(String transport) {
        Counter.builder("order_api_event_publish_failed_total")
                .description("Total number of order event publish failures in the API")
                .tag("transport", transport)
                .register(registry)
                .increment();
    }
}