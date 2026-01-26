package com.orderplatform.api.infrastructure.web;

import com.orderplatform.api.application.usecase.ReprocessDlqEntryUseCaseTx;
import com.orderplatform.core.application.usecase.ListDlqEntriesUseCase;
import com.orderplatform.api.infrastructure.web.dto.DlqEntryResponse;
import com.orderplatform.core.application.model.OrderDlqEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class DlqController {

    private final ListDlqEntriesUseCase listDlqEntriesUseCase;
    private final ReprocessDlqEntryUseCaseTx reprocessDlqEntryUseCase;

    public DlqController(ListDlqEntriesUseCase listDlqEntriesUseCase,
                         ReprocessDlqEntryUseCaseTx reprocessDlqEntryUseCase) {
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

    private DlqEntryResponse toResponse(OrderDlqEntry e) {
        return new DlqEntryResponse(
                e.orderId(),
                e.reason(),
                e.retryCount(),
                e.failedAt()
        );
    }

    @PostMapping("/dlq/{orderId}/reprocess")
    public ResponseEntity<Void> reprocess(@PathVariable UUID orderId) {
        reprocessDlqEntryUseCase.execute(orderId);
        return ResponseEntity.accepted().build();
    }
}

