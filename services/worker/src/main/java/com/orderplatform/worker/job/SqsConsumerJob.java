package com.orderplatform.worker.job;

import com.orderplatform.core.application.usecase.ProcessOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import com.orderplatform.worker.infrastructure.observability.WorkerMetrics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "worker.mode", havingValue = "sqs-consumer")
public class SqsConsumerJob {

    private static final Logger log = LoggerFactory.getLogger(SqsConsumerJob.class);

    private final SqsClient sqsClient;
    private final ProcessOrderUseCase processOrderUseCase;
    private final String queueUrl;


    @Value("${order.worker.maxRetries:3}")
    private int maxRetries;


    private final WorkerMetrics metrics;

    public SqsConsumerJob(
            SqsClient sqsClient,
            ProcessOrderUseCase processOrderUseCase,
            WorkerMetrics metrics,
            @Value("${aws.sqs.queueUrl}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.processOrderUseCase = processOrderUseCase;
        this.metrics = metrics;
        this.queueUrl = queueUrl;
        log.info("SqsConsumerJob ENABLED. queueUrl={}", queueUrl);
    }

    @Scheduled(fixedDelayString = "${worker.sqs.poll.delay-ms:1000}")
    @Transactional
    public void poll() {
        ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(1)
                .messageAttributeNames("All")
                .build();

        metrics.incPoll("sqs");
        metrics.incSqsReceive();

        final List<Message> messages;
        try {
            messages = sqsClient.receiveMessage(req).messages();
        } catch (Exception ex) {
            metrics.incPollError("sqs", "unknown");
            log.error("SQS receiveMessage failed. err={}", ex.toString());
            return;
        }

        metrics.incReceived(messages.size());
        if (messages.isEmpty()) {
            metrics.incSqsReceiveEmpty();
            return;
        }

        Message m = messages.get(0);

        String cid = null;
        if (m.messageAttributes() != null && m.messageAttributes().containsKey("correlationId")) {
            cid = m.messageAttributes().get("correlationId").stringValue();
        }

        if (cid != null && !cid.isBlank()) MDC.put("correlationId", cid);
        try {
            metrics.recordProcessing(() -> {
                UUID orderId = extractOrderId(m.body());
                log.info("SQS message received. messageId={} orderId={}", m.messageId(), orderId);

                Instant now = Instant.now();
                ProcessOrderUseCase.Outcome outcome =
                        processOrderUseCase.execute(orderId, now, maxRetries);

                if (outcome == ProcessOrderUseCase.Outcome.PROCESSED) {
                    try {
                        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(m.receiptHandle())
                                .build());
                        metrics.incSqsDeleteOk();
                        metrics.incProcessed();
                        log.info("Order {} PROCESSED. SQS message deleted. messageId={}", orderId, m.messageId());
                    } catch (Exception ex) {
                        metrics.incSqsDeleteFail();
                        metrics.incFailed();
                        log.error("SQS deleteMessage failed. messageId={} orderId={} err={}", m.messageId(), orderId, ex.toString());
                        // Message not deleted -> will be redelivered (expected)
                    }

                } else if (outcome == ProcessOrderUseCase.Outcome.RETRY) {
                    metrics.incFailed();
                    metrics.incRetried();
                    log.warn("Order {} RETRY. SQS message kept for redelivery after visibility timeout.", orderId);

                } else {
                    // FAILED terminal by business logic. SQS will move to DLQ after maxReceiveCount.
                    metrics.incFailed();
                    metrics.incTerminalFailure();
                    log.error("Order {} FAILED. SQS message kept; it will go to DLQ after maxReceiveCount.", orderId);
                }
            });

        } catch (Exception ex) {
            metrics.incFailed();
            log.error("Error handling SQS messageId={}. Message kept for redelivery. err={}", m.messageId(), ex.toString());
        } finally {
            MDC.remove("correlationId");
        }
    }


    private UUID extractOrderId(String body) {
        // {"orderId":"..."}
        int i = body.indexOf("\"orderId\"");
        if (i < 0) throw new IllegalArgumentException("Missing orderId in body: " + body);
        int colon = body.indexOf(':', i);
        int q1 = body.indexOf('"', colon + 1);
        int q2 = body.indexOf('"', q1 + 1);
        String id = body.substring(q1 + 1, q2);
        return UUID.fromString(id);
    }
}
