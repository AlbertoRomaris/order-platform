package com.orderplatform.core.application.port;

import com.orderplatform.core.domain.order.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);
}
