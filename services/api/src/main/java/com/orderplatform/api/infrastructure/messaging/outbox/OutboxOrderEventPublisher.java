package com.orderplatform.api.infrastructure.messaging.outbox;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.port.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "order.events.mode", havingValue = "outbox")
public class OutboxOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxOrderEventPublisher.class);

    private final OutboxRepository outboxRepository;

    public OutboxOrderEventPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void publishOrderCreated(UUID orderId) {
        String payload = "{\"orderId\":\"" + orderId + "\"}";
        OutboxEvent event = OutboxEvent.pending(orderId, "OrderCreated", payload, Instant.now());
        outboxRepository.enqueue(event);
        log.info("Enqueued OrderCreated event for order {} into outbox", orderId);
    }

}
