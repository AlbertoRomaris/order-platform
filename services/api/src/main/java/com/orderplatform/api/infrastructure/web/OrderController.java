package com.orderplatform.api.infrastructure.web;

import com.orderplatform.api.application.usecase.CreateOrderUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> createOrder() {
        UUID orderId = createOrderUseCase.create();

        // 202 Accepted: async
        return ResponseEntity.accepted().body(Map.of("orderId", orderId));
    }
}
