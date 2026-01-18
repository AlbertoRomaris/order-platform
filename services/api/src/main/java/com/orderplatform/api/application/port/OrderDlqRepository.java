package com.orderplatform.api.application.port;

import com.orderplatform.api.infrastructure.persistence.OrderDlqEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDlqRepository {
    void save(UUID orderId, String reason, int retryCount);
    List<OrderDlqEntity> findAll();
    List<OrderDlqEntity> findLatest(int limit);
    Optional<OrderDlqEntity> findByOrderId(UUID orderId);
    void deleteByOrderId(UUID orderId);

}
