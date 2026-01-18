package com.orderplatform.api.infrastructure.worker;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.api.infrastructure.messaging.InMemoryOrderQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.orderplatform.core.domain.order.OrderStatus;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;

@Component
public class OrderWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderWorker.class);

    @Value("${order.worker.maxRetries:3}")
    private int maxRetries;

    @Value("${order.worker.failureProbability:0.0}")
    private double failureProbability;

    @Value("${order.worker.processingDelayMs:500}")
    private long processingDelayMs;

    @Value("${order.worker.retryDelayMs:1000}")
    private long retryDelayMs;


    private final InMemoryOrderQueue queue;
    private final OrderRepository orderRepository;
    private final OrderDlqRepository dlqRepository;

    public OrderWorker(InMemoryOrderQueue queue, OrderRepository orderRepository, OrderDlqRepository dlqRepository) {
        this.queue = queue;
        this.orderRepository = orderRepository;
        this.dlqRepository = dlqRepository;
    }

    @PostConstruct
    public void start() {
        Thread workerThread = new Thread(this::runLoop, "order-worker");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("OrderWorker started");
    }

    private void runLoop() {
        while (true) {
            try {
                UUID orderId = queue.take();
                handle(orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker loop error", e);
            }
        }
    }

    private void handle(UUID orderId) {
        log.info("Worker received OrderCreated(orderId={})", orderId);

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        try {
            // PENDING -> PROCESSING
            order.markProcessing(Instant.now());
            orderRepository.save(order);
            log.info("Order {} -> PROCESSING", orderId);

            // ---- WORK SIMULATION ----

            if (Math.random() < failureProbability) {
                throw new RuntimeException("Simulated failure for testing");
            }


            Thread.sleep(processingDelayMs);


            // PROCESSING -> PROCESSED
            order.markProcessed(Instant.now());
            orderRepository.save(order);
            log.info("Order {} -> PROCESSED", orderId);

        } catch (Exception ex) {
            String reason = (ex.getMessage() == null || ex.getMessage().isBlank())
                    ? "processing error"
                    : ex.getMessage();

            log.warn("Order {} processing failed: {}", orderId, reason);

            if (order.getStatus() != OrderStatus.PROCESSING) {
                log.warn("Skip failure transition for order {} because status is {} (expected PROCESSING)",
                        orderId, order.getStatus());
                return;
            }

            boolean willExceedRetries = (order.getRetryCount() + 1) >= maxRetries;

            log.warn("Retry decision: currentRetryCount={}, willExceedRetries={}", order.getRetryCount(), (order.getRetryCount() + 1) >= maxRetries);


            if (!willExceedRetries) {
                // fallo recuperable -> vuelve a PENDING
                order.markRetryableFailure(reason, Instant.now());
                orderRepository.save(order);
                log.info("Order {} -> PENDING for retry (retryCount={})", orderId, order.getRetryCount());

                sleepQuietly(retryDelayMs);
                queue.publish(orderId);
                log.info("Re-enqueued order {} for retry {}", orderId, order.getRetryCount());

            } else {
                // fallo terminal -> FAILED + DLQ
                order.markFailed(reason, Instant.now());
                orderRepository.save(order);
                log.info("Order {} -> FAILED terminal (retryCount={})", orderId, order.getRetryCount());

                dlqRepository.save(orderId, reason, order.getRetryCount());
                log.error("Order {} sent to DLQ after {} retries", orderId, order.getRetryCount());
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
