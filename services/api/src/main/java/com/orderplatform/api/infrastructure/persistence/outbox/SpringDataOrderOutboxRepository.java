package com.orderplatform.api.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOrderOutboxRepository extends JpaRepository<OrderOutboxEntity, UUID> {
}
