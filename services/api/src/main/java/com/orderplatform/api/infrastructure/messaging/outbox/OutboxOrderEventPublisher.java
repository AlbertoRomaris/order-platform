package com.orderplatform.api.infrastructure.messaging.outbox;

import com.orderplatform.api.infrastructure.observability.ApiMetrics;
import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.port.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "order.events.mode", havingValue = "outbox")
public class OutboxOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxOrderEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ApiMetrics apiMetrics;

    public OutboxOrderEventPublisher(OutboxRepository outboxRepository, ApiMetrics apiMetrics) {
        this.outboxRepository = outboxRepository;
        this.apiMetrics = apiMetrics;
    }

    @Override
    public void publishOrderCreated(UUID orderId) {
        String correlationId = MDC.get("correlationId");

        String payload = """
    {"orderId":"%s","correlationId":%s}
    """.formatted(
                orderId,
                correlationId == null ? "null" : "\"" + correlationId + "\""
        ).trim();

        OutboxEvent event = OutboxEvent.pending(orderId, "OrderCreated", payload, Instant.now());

        try {
            outboxRepository.enqueue(event);
            apiMetrics.incEventPublished("outbox");
            log.info("Enqueued OrderCreated event for order {} into outbox", orderId);
        } catch (Exception ex) {
            apiMetrics.incEventPublishFailed("outbox");
            throw ex;
        }
    }

}
