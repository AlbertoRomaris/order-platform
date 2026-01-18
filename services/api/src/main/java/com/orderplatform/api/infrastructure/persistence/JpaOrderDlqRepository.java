package com.orderplatform.api.infrastructure.persistence;

import com.orderplatform.api.application.port.OrderDlqRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;

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
    public List<OrderDlqEntity> findAll() {
        return springRepo.findAll();
    }

    @Override
    public List<OrderDlqEntity> findLatest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200)); // evitamos locuras tipo 1M
        return springRepo.findByOrderByFailedAtDesc(PageRequest.of(0, safeLimit));
    }

    @Override
    public void deleteByOrderId(UUID orderId) {
        springRepo.deleteById(orderId);
    }

    @Override
    public Optional<OrderDlqEntity> findByOrderId(UUID orderId) {
        return springRepo.findById(orderId);
    }
}
