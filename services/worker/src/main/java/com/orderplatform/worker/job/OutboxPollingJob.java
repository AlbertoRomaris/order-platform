package com.orderplatform.worker.job;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OutboxRepository;
import com.orderplatform.core.application.usecase.ProcessOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import com.orderplatform.worker.infrastructure.observability.WorkerMetrics;



import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "worker.mode", havingValue = "outbox-processor", matchIfMissing = true)
public class OutboxPollingJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingJob.class);

    private final OutboxRepository outboxRepository;
    private final ProcessOrderUseCase processOrderUseCase;
    private final ObjectMapper objectMapper;

    private final String workerId = "worker-1";

    @Value("${order.worker.maxRetries:3}")
    private int maxRetries;

    @Value("${worker.outbox.retryDelayMs:1000}")
    private long retryDelayMs;

    @Value("${worker.outbox.lockTimeoutSeconds:30}")
    private long lockTimeoutSeconds;

    private final WorkerMetrics metrics;


    public OutboxPollingJob(OutboxRepository outboxRepository,
                            ProcessOrderUseCase processOrderUseCase,
                            ObjectMapper objectMapper,
                            WorkerMetrics metrics) {
        this.outboxRepository = outboxRepository;
        this.processOrderUseCase = processOrderUseCase;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }


    private String extractCorrelationId(String payload) {
        if (payload == null || payload.isBlank()) return null;

        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode cid = node.get("correlationId");
            if (cid == null || cid.isNull()) return null;
            String v = cid.asText();
            return (v == null || v.isBlank()) ? null : v;
        } catch (Exception ex) {
            // Payload malformado no debe tumbar el worker
            log.warn("Could not parse correlationId from payload. payload={}", payload);
            return null;
        }
    }


    @Scheduled(fixedDelayString = "${worker.outbox.poll.delay-ms:1000}")
    @Transactional
    public void poll() {
        metrics.pollTotal.increment();
        Instant now = Instant.now();
        Instant olderThan = now.minusSeconds(lockTimeoutSeconds);
        int released = outboxRepository.releaseStaleLocks(olderThan);
        if (released > 0) {
            log.warn("Released {} stale outbox locks older than {} seconds", released, lockTimeoutSeconds);
        }

        List<OutboxEvent> events = outboxRepository.claimReady(5, now, workerId);
        metrics.messagesReceivedTotal.increment(events.size());

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent e : events) {
            UUID eventId = e.id();
            UUID orderId = e.aggregateId();

            String correlationId = extractCorrelationId(e.payload());
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }

            try {
                metrics.processingTimer.record(() -> {

                    ProcessOrderUseCase.Outcome outcome =
                            processOrderUseCase.execute(orderId, now, maxRetries);

                    if (outcome == ProcessOrderUseCase.Outcome.PROCESSED) {
                        outboxRepository.markProcessed(eventId, Instant.now());
                        metrics.messagesProcessedTotal.increment();
                        log.info("Order {} PROCESSED; outbox {} PROCESSED", orderId, eventId);

                    } else if (outcome == ProcessOrderUseCase.Outcome.RETRY) {
                        Instant nextAttemptAt = Instant.now().plusMillis(retryDelayMs);
                        outboxRepository.reschedule(eventId, nextAttemptAt, "retry");
                        metrics.messagesFailedTotal.increment();
                        metrics.messagesRetriedTotal.increment();
                        log.warn("Order {} RETRY; outbox {} rescheduled for {}", orderId, eventId, nextAttemptAt);

                    } else {
                        outboxRepository.markFailed(eventId, Instant.now(), "failed");
                        metrics.messagesFailedTotal.increment();
                        log.error("Order {} FAILED; outbox {} marked FAILED", orderId, eventId);
                    }
                });
            } finally {
                MDC.remove("correlationId");
            }
        }

    }
}
