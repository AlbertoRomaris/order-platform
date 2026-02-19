package com.orderplatform.worker.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetrics {

    public final Counter pollTotal;
    public final Counter messagesReceivedTotal;

    public final Counter messagesProcessedTotal;
    public final Counter messagesFailedTotal;
    public final Counter messagesRetriedTotal;

    public final Counter businessDlqTotal; // DB order_dlq
    public final Counter sqsDlqTotal;      // SQS redrive DLQ

    public final Timer processingTimer;

    public final Counter relayPublishedTotal;
    public final Counter relayFailedTotal;


    public WorkerMetrics(MeterRegistry registry) {
        this.pollTotal = Counter.builder("order_worker_poll_total")
                .description("Total number of worker poll cycles (SQS long poll or outbox poll)")
                .register(registry);

        this.messagesReceivedTotal = Counter.builder("order_worker_messages_received_total")
                .description("Total number of events/messages received by the worker")
                .register(registry);

        this.messagesProcessedTotal = Counter.builder("order_worker_messages_processed_total")
                .description("Total number of events/messages processed successfully by the worker")
                .register(registry);

        this.messagesFailedTotal = Counter.builder("order_worker_messages_failed_total")
                .description("Total number of processing attempts that failed")
                .register(registry);

        this.messagesRetriedTotal = Counter.builder("order_worker_messages_retried_total")
                .description("Total number of processing attempts that resulted in a retry")
                .register(registry);

        this.businessDlqTotal = Counter.builder("order_worker_business_dlq_total")
                .description("Total number of business DLQ entries created in DB (order_dlq)")
                .register(registry);

        this.sqsDlqTotal = Counter.builder("order_worker_sqs_dlq_total")
                .description("Total number of terminal failures that will end up in the SQS DLQ (redrive)")
                .register(registry);

        this.processingTimer = Timer.builder("order_worker_processing_seconds")
                .description("Time spent processing a single message/event")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.relayPublishedTotal = Counter.builder("order_worker_relay_published_total")
                .description("Total number of outbox events relayed to SQS successfully")
                .register(registry);

        this.relayFailedTotal = Counter.builder("order_worker_relay_failed_total")
                .description("Total number of outbox relay attempts that failed")
                .register(registry);

    }
}
