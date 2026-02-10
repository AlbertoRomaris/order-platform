package com.orderplatform.worker.job;

import com.orderplatform.core.application.model.OutboxEvent;
import com.orderplatform.core.application.port.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNullElse;

@Component
@ConditionalOnProperty(name = "worker.mode", havingValue = "outbox-relay")
public class OutboxToSqsRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxToSqsRelayJob.class);

    private final OutboxRepository outboxRepository;
    private final SqsClient sqsClient;
    private final String queueUrl;

    private final String workerId = "worker-relay-1";

    public OutboxToSqsRelayJob(
            OutboxRepository outboxRepository,
            SqsClient sqsClient,
            @Value("${aws.sqs.queueUrl}") String queueUrl
    ) {
        this.outboxRepository = outboxRepository;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelayString = "${worker.outbox.poll.delay-ms:1000}")
    @Transactional
    public void pollAndRelay() {
        var now = java.time.Instant.now();
        List<OutboxEvent> events = outboxRepository.claimReady(5, now, workerId);
        if (events.isEmpty()) return;

        for (OutboxEvent e : events) {
            var eventId = e.id();
            var payload = requireNonNullElse(e.payload(), "");

            // claim lock
            outboxRepository.markProcessing(eventId, now, workerId);

            try {
                // correlationId from payload (simple parse, best-effort)
                String correlationId = extractCorrelationId(payload);

                SendMessageRequest.Builder req = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(payload.isBlank()
                                ? "{\"orderId\":\"" + e.aggregateId() + "\"}"
                                : payload);

                if (correlationId != null && !correlationId.isBlank()) {
                    req.messageAttributes(Map.of(
                            "correlationId",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(correlationId)
                                    .build()
                    ));
                }

                sqsClient.sendMessage(req.build());

                outboxRepository.markProcessed(eventId, java.time.Instant.now());
                log.info("Relayed outbox {} to SQS. aggregateId={}", eventId, e.aggregateId());

            } catch (Exception ex) {
                outboxRepository.reschedule(eventId, java.time.Instant.now().plusSeconds(5), ex.getMessage());
                log.warn("Relay failed for outbox {}. Rescheduled. err={}", eventId, ex.toString());
            }
        }
    }

    // best-effort tiny parser without adding Jackson here (to keep job light)
    private String extractCorrelationId(String payload) {
        // expects ..."correlationId":"XYZ"...
        int i = payload.indexOf("\"correlationId\"");
        if (i < 0) return null;
        int colon = payload.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = payload.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = payload.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return payload.substring(q1 + 1, q2);
    }
}
