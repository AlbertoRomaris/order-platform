package com.orderplatform.core.application.usecase;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderProcessor;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.domain.order.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public class ProcessOrderUseCase {

    public enum Outcome {
        PROCESSED,
        RETRY,
        FAILED
    }

    private final OrderRepository orderRepository;
    private final OrderDlqRepository dlqRepository;
    private final OrderProcessor processor;

    public ProcessOrderUseCase(OrderRepository orderRepository,
                               OrderDlqRepository dlqRepository,
                               OrderProcessor processor) {
        this.orderRepository = orderRepository;
        this.dlqRepository = dlqRepository;
        this.processor = processor;
    }

    public Outcome execute(UUID orderId, Instant now, int maxRetries) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        try {
            // PENDING -> PROCESSING
            order.markProcessing(now);
            orderRepository.save(order);

            // “Trabajo real” (puede fallar)
            processor.process(orderId);

            // PROCESSING -> PROCESSED
            order.markProcessed(Instant.now());
            orderRepository.save(order);

            return Outcome.PROCESSED;

        } catch (Exception ex) {
            String reason = (ex.getMessage() == null || ex.getMessage().isBlank())
                    ? "processing error"
                    : ex.getMessage();

            if (order.getStatus() != OrderStatus.PROCESSING) {
                // Si no llegó a PROCESSING, no hacemos transición de fallo
                return Outcome.FAILED;
            }

            boolean willExceedRetries = (order.getRetryCount() + 1) >= maxRetries;

            if (!willExceedRetries) {
                order.markRetryableFailure(reason, Instant.now());
                orderRepository.save(order);
                return Outcome.RETRY;
            } else {
                order.markFailed(reason, Instant.now());
                orderRepository.save(order);

                dlqRepository.save(orderId, reason, order.getRetryCount());
                return Outcome.FAILED;
            }
        }
    }
}
