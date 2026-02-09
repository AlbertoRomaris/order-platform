package com.orderplatform.api.infrastructure.messaging;

import com.orderplatform.core.application.port.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@ConditionalOnProperty(name = "order.events.mode", havingValue = "inmemory", matchIfMissing = true)
@Component
public class InMemoryOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryOrderEventPublisher.class);

    private final InMemoryOrderQueue queue;

    public InMemoryOrderEventPublisher(InMemoryOrderQueue queue) {
        this.queue = queue;
    }

    @Override
    public void publishOrderCreated(UUID orderId) {
        queue.publish(orderId);
        log.info("Enqueued event: OrderCreated(orderId={})", orderId);
    }
}

