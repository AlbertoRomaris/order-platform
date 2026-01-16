package com.orderplatform.api.infrastructure.worker;

import com.orderplatform.api.application.port.OrderRepository;
import com.orderplatform.api.domain.order.Order;
import com.orderplatform.api.infrastructure.messaging.InMemoryOrderQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
public class OrderWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderWorker.class);

    private final InMemoryOrderQueue queue;
    private final OrderRepository orderRepository;

    public OrderWorker(InMemoryOrderQueue queue, OrderRepository orderRepository) {
        this.queue = queue;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void start() {
        Thread workerThread = new Thread(this::runLoop, "order-worker");
        workerThread.setDaemon(true); // not blocking shutdown
        workerThread.start();
        log.info("OrderWorker started");
    }

    private void runLoop() {
        while (true) {
            try {
                var orderId = queue.take();
                process(orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // exit loop if we interrupt
            } catch (Exception e) {
                log.error("Worker loop error", e);
            }
        }
    }

    private void process(java.util.UUID orderId) {
        log.info("Worker received OrderCreated(orderId={})", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        // PENDING -> PROCESSING
        order.markProcessing(Instant.now());
        orderRepository.save(order);
        log.info("Order {} -> PROCESSING", orderId);

        // Simulación de trabajo
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // PROCESSING -> PROCESSED
        order.markProcessed(Instant.now());
        orderRepository.save(order);
        log.info("Order {} -> PROCESSED", orderId);
    }
}
