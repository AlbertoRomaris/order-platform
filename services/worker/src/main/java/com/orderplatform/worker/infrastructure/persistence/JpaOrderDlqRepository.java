package com.orderplatform.worker.infrastructure.persistence;

import com.orderplatform.core.application.model.OrderDlqEntry;
import com.orderplatform.core.application.port.OrderDlqRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderDlqRepository implements OrderDlqRepository {

    private final SpringDataOrderDlqRepository springRepo;

    public JpaOrderDlqRepository(SpringDataOrderDlqRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(UUID orderId, String reason, int retryCount) {
        var entity = new OrderDlqEntity(
                orderId,
                reason,
                retryCount,
                Instant.now()
        );
        springRepo.save(entity);
    }

    @Override
    public List<OrderDlqEntry> findAll() {
        return springRepo.findAll()
                .stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    public List<OrderDlqEntry> findLatest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200)); // evitamos locuras tipo 1M
        return springRepo.findByOrderByFailedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    public Optional<OrderDlqEntry> findByOrderId(UUID orderId) {
        return springRepo.findById(orderId).map(this::toEntry);
    }

    @Override
    public void deleteByOrderId(UUID orderId) {
        springRepo.deleteById(orderId);
    }

    private OrderDlqEntry toEntry(OrderDlqEntity e) {
        return new OrderDlqEntry(
                e.getOrderId(),
                e.getReason(),
                e.getRetryCount(),
                e.getFailedAt()
        );
    }
}

