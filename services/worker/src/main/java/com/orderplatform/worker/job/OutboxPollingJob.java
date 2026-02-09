package com.orderplatform.worker.job;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OutboxRepository;
import com.orderplatform.core.application.usecase.ProcessOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxPollingJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingJob.class);

    private final OutboxRepository outboxRepository;
    private final ProcessOrderUseCase processOrderUseCase;

    private final String workerId = "worker-1";

    @Value("${order.worker.maxRetries:3}")
    private int maxRetries;

    @Value("${worker.outbox.retryDelayMs:1000}")
    private long retryDelayMs;

    public OutboxPollingJob(OutboxRepository outboxRepository,
                            ProcessOrderUseCase processOrderUseCase) {
        this.outboxRepository = outboxRepository;
        this.processOrderUseCase = processOrderUseCase;
    }

    @Scheduled(fixedDelayString = "${worker.outbox.poll.delay-ms:1000}")
    @Transactional
    public void poll() {
        Instant now = Instant.now();
        List<OutboxEvent> events = outboxRepository.claimReady(5, now, workerId);

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent e : events) {
            UUID eventId = e.id();
            UUID orderId = e.aggregateId();

            // 1) Claim/lock
            outboxRepository.markProcessing(eventId, now, workerId);

            // 2) Real Work (state order + DLQ/retryCount)
            ProcessOrderUseCase.Outcome outcome =
                    processOrderUseCase.execute(orderId, now, maxRetries);

            // 3) ONLY if work is completely and successfully
            if (outcome == ProcessOrderUseCase.Outcome.PROCESSED) {
                outboxRepository.markProcessed(eventId, Instant.now());
                log.info("Order {} PROCESSED; outbox {} PROCESSED", orderId, eventId);

            } else if (outcome == ProcessOrderUseCase.Outcome.RETRY) {
                Instant nextAttemptAt = Instant.now().plusMillis(retryDelayMs);
                outboxRepository.reschedule(eventId, nextAttemptAt, "retry");
                log.warn("Order {} RETRY; outbox {} rescheduled for {}", orderId, eventId, nextAttemptAt);

            } else {
                outboxRepository.markFailed(eventId, Instant.now(), "failed");
                log.error("Order {} FAILED; outbox {} marked FAILED", orderId, eventId);
            }

        }
    }
}
