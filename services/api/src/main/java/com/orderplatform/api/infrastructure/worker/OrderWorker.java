package com.orderplatform.api.infrastructure.worker;

import com.orderplatform.api.infrastructure.messaging.InMemoryOrderQueue;
import com.orderplatform.core.application.port.OrderProcessor;
import com.orderplatform.core.application.usecase.ProcessOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;

@ConditionalOnProperty(name = "order.events.mode", havingValue = "inmemory", matchIfMissing = true)
@Component
public class OrderWorker{

    private static final Logger log = LoggerFactory.getLogger(OrderWorker.class);

    @Value("${order.worker.maxRetries:3}")
    private int maxRetries;


    @Value("${order.worker.retryDelayMs:1000}")
    private long retryDelayMs;

    private final InMemoryOrderQueue queue;
    private final ProcessOrderUseCase processOrderUseCase;

    public OrderWorker(InMemoryOrderQueue queue, ProcessOrderUseCase processOrderUseCase) {
        this.queue = queue;
        this.processOrderUseCase = processOrderUseCase;
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
                log.info("Worker received OrderCreated(orderId={})", orderId);

                ProcessOrderUseCase.Outcome outcome =
                        processOrderUseCase.execute(orderId, Instant.now(), maxRetries);

                if (outcome == ProcessOrderUseCase.Outcome.RETRY) {
                    sleepQuietly(retryDelayMs);
                    queue.publish(orderId);
                    log.info("Re-enqueued order {} for retry", orderId);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker loop error", e);
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
