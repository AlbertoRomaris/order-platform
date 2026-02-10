package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.usecase.CreateOrderUseCase;
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

    public CreateOrderUseCaseTx(CreateOrderUseCase useCase,
                                OrderEventPublisher eventPublisher) {
        this.useCase = useCase;
        this.eventPublisher = eventPublisher;
    }


    @Transactional
    public UUID execute() {
        Instant now = Instant.now();
        UUID orderId = useCase.execute(now);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishOrderCreated(orderId);
                    }
                }
        );

        return orderId;
    }

}

