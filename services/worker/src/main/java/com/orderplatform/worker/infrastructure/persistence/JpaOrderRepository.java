package com.orderplatform.worker.infrastructure.persistence;


import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.domain.order.Order;
import com.orderplatform.core.domain.order.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springRepo;

    public JpaOrderRepository(SpringDataOrderRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(Order order) {
        OrderEntity entity = new OrderEntity(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getRetryCount(),
                order.getFailureReason()
        );
        springRepo.save(entity);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return springRepo.findById(id).map(entity ->
                Order.restore(
                        entity.getId(),
                        OrderStatus.valueOf(entity.getStatus()),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getRetryCount(),
                        entity.getFailureReason()
                )
        );
    }
}
