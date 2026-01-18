package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.port.OrderEventPublisher;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.domain.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public CreateOrderUseCase(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID create() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.newPending(orderId, Instant.now());

        orderRepository.save(order);

        // IMPORTANT: publish only AFTER the DB transaction is committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishOrderCreated(orderId);
            }
        });

        return orderId;
    }
}
