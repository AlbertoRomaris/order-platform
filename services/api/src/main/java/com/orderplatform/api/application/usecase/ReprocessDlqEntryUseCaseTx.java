package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.usecase.ReprocessDlqEntryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReprocessDlqEntryUseCaseTx {

    private final ReprocessDlqEntryUseCase useCase;
    private final OrderEventPublisher eventPublisher;

    public ReprocessDlqEntryUseCaseTx(ReprocessDlqEntryUseCase useCase,
                                      OrderEventPublisher eventPublisher) {
        this.useCase = useCase;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID orderId) {
        UUID idToPublish = useCase.execute(orderId, Instant.now());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishOrderCreated(idToPublish);
            }
        });
    }
}
