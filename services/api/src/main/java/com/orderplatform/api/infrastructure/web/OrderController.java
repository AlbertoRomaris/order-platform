package com.orderplatform.api.infrastructure.web;

import com.orderplatform.api.application.usecase.CreateOrderUseCaseTx;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateOrderUseCaseTx createOrderUseCase;

    public OrderController(CreateOrderUseCaseTx createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> createOrder() {
        UUID orderId = createOrderUseCase.execute();

        // 202 Accepted: async
        return ResponseEntity.accepted().body(Map.of("orderId", orderId));
    }
}
