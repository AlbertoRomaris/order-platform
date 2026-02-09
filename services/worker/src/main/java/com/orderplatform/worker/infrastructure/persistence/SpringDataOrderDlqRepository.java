package com.orderplatform.worker.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderDlqRepository
        extends JpaRepository<OrderDlqEntity, UUID> {

    List<OrderDlqEntity> findByOrderByFailedAtDesc(Pageable pageable);

    Optional<OrderDlqEntity> findByOrderId(UUID orderId);
}
