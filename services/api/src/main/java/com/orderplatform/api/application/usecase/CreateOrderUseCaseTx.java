package com.orderplatform.api.application.usecase;

import com.orderplatform.api.infrastructure.observability.ApiMetrics;
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
    private final ApiMetrics apiMetrics;

    public CreateOrderUseCaseTx(CreateOrderUseCase useCase,
                                OrderEventPublisher eventPublisher,
                                ApiMetrics apiMetrics) {
        this.useCase = useCase;
        this.eventPublisher = eventPublisher;
        this.apiMetrics = apiMetrics;
    }


    @Transactional
    public UUID execute() {
        try {
            return apiMetrics.recordCreateOrder(() -> {

                Instant now = Instant.now();
                UUID orderId = useCase.execute(now);

                apiMetrics.incOrdersCreated();

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                eventPublisher.publishOrderCreated(orderId);
                            }
                        }
                );

                return orderId;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

