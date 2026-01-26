package com.orderplatform.core.application.usecase;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderRepository;

import java.time.Instant;
import java.util.UUID;

public class ReprocessDlqEntryUseCase {

    private final OrderDlqRepository dlqRepository;
    private final OrderRepository orderRepository;

    public ReprocessDlqEntryUseCase(OrderDlqRepository dlqRepository,
                                    OrderRepository orderRepository) {
        this.dlqRepository = dlqRepository;
        this.orderRepository = orderRepository;
    }

    public UUID execute(UUID orderId, Instant now) {
        dlqRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "DLQ entry not found for orderId: " + orderId
                ));

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + orderId
                ));

        dlqRepository.deleteByOrderId(orderId);

        order.resetToPending("manual reprocess", now);
        orderRepository.save(order);

        return orderId;
    }
}
