package com.orderplatform.core.application.usecase;

import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.domain.order.Order;

import java.time.Instant;
import java.util.UUID;

public class CreateOrderUseCase {

    private final OrderRepository orderRepository;

    public CreateOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public UUID execute(Instant now) {
        UUID orderId = UUID.randomUUID();
        Order order = Order.newPending(orderId, now);
        orderRepository.save(order);
        return orderId;
    }
}
