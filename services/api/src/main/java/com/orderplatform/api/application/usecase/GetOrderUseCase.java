package com.orderplatform.api.application.usecase;

import com.orderplatform.api.application.port.OrderRepository;
import com.orderplatform.core.domain.order.Order;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> execute(UUID id) {
        return orderRepository.findById(id);
    }
}
