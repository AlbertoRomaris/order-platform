package com.orderplatform.api.application.usecase;

import com.orderplatform.api.application.port.OrderDlqRepository;
import com.orderplatform.api.infrastructure.persistence.OrderDlqEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListDlqEntriesUseCase {

    private final OrderDlqRepository dlqRepository;

    public ListDlqEntriesUseCase(OrderDlqRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    public List<OrderDlqEntity> execute(int limit) {
        return dlqRepository.findLatest(limit);
    }
}
