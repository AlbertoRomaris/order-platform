package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReprocessDlqEntryUseCase {

    private final OrderDlqRepository dlqRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public ReprocessDlqEntryUseCase(OrderDlqRepository dlqRepository,
                                    OrderRepository orderRepository,
                                    OrderEventPublisher eventPublisher) {
        this.dlqRepository = dlqRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID orderId) {
        // 1) exists in DLQ?
        dlqRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "DLQ entry not found for orderId: " + orderId
                ));

        // 2) load order
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + orderId
                ));

        // 3) delete DLQ entry
        dlqRepository.deleteByOrderId(orderId);

        // 4) reset order to PENDING
        order.resetToPending("manual reprocess", Instant.now());
        orderRepository.save(order);

        // 5) publish AFTER COMMIT (important!)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishOrderCreated(orderId);
            }
        });
    }
}
