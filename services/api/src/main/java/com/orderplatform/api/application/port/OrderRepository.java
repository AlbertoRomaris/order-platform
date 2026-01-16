package com.orderplatform.api.application.port;

import com.orderplatform.api.domain.order.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);
}
