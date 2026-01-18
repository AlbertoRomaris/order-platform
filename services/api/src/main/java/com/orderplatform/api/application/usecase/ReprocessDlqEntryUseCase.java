package com.orderplatform.api.application.usecase;

import com.orderplatform.api.application.port.OrderDlqRepository;
import com.orderplatform.api.application.port.OrderRepository;
import com.orderplatform.api.domain.order.Order;
import com.orderplatform.api.infrastructure.messaging.InMemoryOrderQueue;
import com.orderplatform.api.infrastructure.persistence.OrderDlqEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReprocessDlqEntryUseCase {

    private final OrderDlqRepository dlqRepository;
    private final OrderRepository orderRepository;
    private final InMemoryOrderQueue queue;

    public ReprocessDlqEntryUseCase(OrderDlqRepository dlqRepository,
                                    OrderRepository orderRepository,
                                    InMemoryOrderQueue queue) {
        this.dlqRepository = dlqRepository;
        this.orderRepository = orderRepository;
        this.queue = queue;
    }

    @Transactional
    public void execute(UUID orderId) {
        // 1) exists in DLQ?
        var dlqEntry = dlqRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "DLQ entry not found for orderId: " + orderId
                ));

        // 2) reset order
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + orderId
                ));

        // 3) delete DLQ
        dlqRepository.deleteByOrderId(orderId);

        // 4) reset order to PENDING
        order.resetToPending("manual reprocess", Instant.now());
        orderRepository.save(order);

        // 5) re-encolar
        queue.publish(orderId);
    }
}
