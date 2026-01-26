package com.orderplatform.core.application.usecase;

import com.orderplatform.core.application.model.OrderDlqEntry;
import com.orderplatform.core.application.port.OrderDlqRepository;

import java.util.List;

public class ListDlqEntriesUseCase {

    private final OrderDlqRepository dlqRepository;

    public ListDlqEntriesUseCase(OrderDlqRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    public List<OrderDlqEntry> execute(int limit) {
        return dlqRepository.findLatest(limit);
    }
}

