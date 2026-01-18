package com.orderplatform.api.application.usecase;

import com.orderplatform.core.application.model.OrderDlqEntry;
import com.orderplatform.core.application.port.OrderDlqRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListDlqEntriesUseCase {

    private final OrderDlqRepository dlqRepository;

    public ListDlqEntriesUseCase(OrderDlqRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    public List<OrderDlqEntry> execute(int limit) {
        return dlqRepository.findLatest(limit);
    }
}

