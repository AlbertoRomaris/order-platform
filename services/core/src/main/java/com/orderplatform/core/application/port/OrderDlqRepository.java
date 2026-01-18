package com.orderplatform.core.application.port;

import com.orderplatform.core.application.model.OrderDlqEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDlqRepository {

    void save(UUID orderId, String reason, int retryCount);

    List<OrderDlqEntry> findAll();

    List<OrderDlqEntry> findLatest(int limit);

    Optional<OrderDlqEntry> findByOrderId(UUID orderId);

    void deleteByOrderId(UUID orderId);
}

