package com.orderplatform.core.application.usecase;

import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.domain.order.Order;

import java.util.Optional;
import java.util.UUID;


public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> execute(UUID id) {
        return orderRepository.findById(id);
    }
}
