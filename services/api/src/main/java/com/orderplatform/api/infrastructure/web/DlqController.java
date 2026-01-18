package com.orderplatform.api.infrastructure.web;

import com.orderplatform.api.application.usecase.ListDlqEntriesUseCase;
import com.orderplatform.api.infrastructure.persistence.OrderDlqEntity;
import com.orderplatform.api.infrastructure.web.dto.DlqEntryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.orderplatform.api.application.usecase.ReprocessDlqEntryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.List;
import java.util.UUID;

@RestController
public class DlqController {

    private final ListDlqEntriesUseCase listDlqEntriesUseCase;
    private final ReprocessDlqEntryUseCase reprocessDlqEntryUseCase;

    public DlqController(ListDlqEntriesUseCase listDlqEntriesUseCase,
                         ReprocessDlqEntryUseCase reprocessDlqEntryUseCase) {
        this.listDlqEntriesUseCase = listDlqEntriesUseCase;
        this.reprocessDlqEntryUseCase = reprocessDlqEntryUseCase;
    }

    @GetMapping("/dlq")
    public List<DlqEntryResponse> list(@RequestParam(defaultValue = "50") int limit) {
        return listDlqEntriesUseCase.execute(limit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DlqEntryResponse toResponse(OrderDlqEntity e) {
        return new DlqEntryResponse(
                e.getOrderId(),
                e.getReason(),
                e.getRetryCount(),
                e.getFailedAt()
        );
    }

    @PostMapping("/dlq/{orderId}/reprocess")
    public ResponseEntity<Void> reprocess(@PathVariable UUID orderId) {
        reprocessDlqEntryUseCase.execute(orderId);
        return ResponseEntity.accepted().build();
    }

}
