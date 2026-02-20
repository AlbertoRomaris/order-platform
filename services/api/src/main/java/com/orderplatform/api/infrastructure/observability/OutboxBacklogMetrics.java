package com.orderplatform.api.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxBacklogMetrics {

    public OutboxBacklogMetrics(MeterRegistry registry, JdbcTemplate jdbcTemplate) {

        Gauge.builder("order_api_outbox_backlog", () -> countBacklog(jdbcTemplate))
                .description("Number of outbox events pending processing/delivery (PENDING + PROCESSING)")
                .register(registry);

        Gauge.builder("order_api_outbox_processing", () -> countProcessing(jdbcTemplate))
                .description("Number of outbox events currently locked/processing (PROCESSING)")
                .register(registry);
    }

    private static double countBacklog(JdbcTemplate jdbcTemplate) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from order_outbox where status in ('PENDING','PROCESSING')",
                Integer.class
        );
        return n == null ? 0.0 : n.doubleValue();
    }

    private static double countProcessing(JdbcTemplate jdbcTemplate) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from order_outbox where status = 'PROCESSING'",
                Integer.class
        );
        return n == null ? 0.0 : n.doubleValue();
    }
}