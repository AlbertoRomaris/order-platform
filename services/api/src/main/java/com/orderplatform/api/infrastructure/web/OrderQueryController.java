package com.orderplatform.api.infrastructure.web;

import com.orderplatform.api.application.usecase.GetOrderUseCase;
import com.orderplatform.core.domain.order.Order;
import com.orderplatform.api.infrastructure.web.dto.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderQueryController {

    private final GetOrderUseCase getOrderUseCase;

    public OrderQueryController(GetOrderUseCase getOrderUseCase) {
        this.getOrderUseCase = getOrderUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return getOrderUseCase.execute(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getRetryCount(),
                order.getFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
