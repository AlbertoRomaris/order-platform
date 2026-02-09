package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OutboxRepository;
import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.usecase.CreateOrderUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateOrderUseCaseTx {

    private final CreateOrderUseCase useCase;
    private final OrderEventPublisher eventPublisher;
    private final OutboxRepository outboxRepository;
    private final String eventsMode;

    public CreateOrderUseCaseTx(CreateOrderUseCase useCase,
                                OrderEventPublisher eventPublisher,
                                OutboxRepository outboxRepository,
                                @Value("${order.events.mode:inmemory}") String eventsMode) {
        this.useCase = useCase;
        this.eventPublisher = eventPublisher;
        this.outboxRepository = outboxRepository;
        this.eventsMode = eventsMode;
    }

    @Transactional
    public UUID execute() {
        Instant now = Instant.now();
        UUID orderId = useCase.execute(now);

        if ("outbox".equalsIgnoreCase(eventsMode)) {
            OutboxEvent event = OutboxEvent.pending(
                    orderId,
                    "OrderCreated",
                    null,
                    now
            );
            outboxRepository.enqueue(event);
        } else {
            // Mode V1: publicamos afterCommit a la cola in-memory
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishOrderCreated(orderId);
                        }
                    }
            );
        }

        return orderId;
    }
}

