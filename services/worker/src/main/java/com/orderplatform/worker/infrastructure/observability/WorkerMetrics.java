package com.orderplatform.worker.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetrics {

    private final MeterRegistry registry;

    private final Counter messagesReceivedTotal;
    private final Counter messagesProcessedTotal;
    private final Counter messagesFailedTotal;
    private final Counter messagesRetriedTotal;

    // terminal failures (NOT real DLQ)
    private final Counter terminalFailuresTotal;

    private final Timer processingTimer;

    private final Counter relayPublishedTotal;
    private final Counter relayFailedTotal;

    private final Counter sqsReceiveTotal;
    private final Counter sqsReceiveEmptyTotal;

    private final Counter sqsDeleteTotal;
    private final Counter sqsDeleteFailedTotal;

    public WorkerMetrics(MeterRegistry registry) {
        this.registry = registry;

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

        this.terminalFailuresTotal = Counter.builder("order_worker_terminal_failures_total")
                .description("Total number of terminal failures (not real DLQ) expected to be redriven to a DLQ by the transport")
                .tag("transport", "sqs")
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

        this.sqsReceiveTotal = Counter.builder("order_worker_sqs_receive_total")
                .description("Total number of SQS receiveMessage calls executed")
                .register(registry);

        this.sqsReceiveEmptyTotal = Counter.builder("order_worker_sqs_receive_empty_total")
                .description("Total number of SQS receiveMessage calls that returned zero messages")
                .register(registry);

        this.sqsDeleteTotal = Counter.builder("order_worker_sqs_delete_total")
                .description("Total number of SQS deleteMessage calls executed successfully")
                .register(registry);

        this.sqsDeleteFailedTotal = Counter.builder("order_worker_sqs_delete_failed_total")
                .description("Total number of SQS deleteMessage calls that failed")
                .register(registry);
    }

    // ---- Polling ----

    public void incPoll(String mode) {
        registry.counter("order_worker_poll_total", "mode", mode).increment();
    }

    public void incPollError(String mode, String error) {
        registry.counter("order_worker_poll_errors_total", "mode", mode, "error", error).increment();
    }

    public void incReceived(int n) {
        if (n > 0) messagesReceivedTotal.increment(n);
    }

    public void incProcessed() {
        messagesProcessedTotal.increment();
    }

    public void incFailed() {
        messagesFailedTotal.increment();
    }

    public void incRetried() {
        messagesRetriedTotal.increment();
    }

    public void incTerminalFailure() {
        terminalFailuresTotal.increment();
    }

    public void incSqsReceive() {
        sqsReceiveTotal.increment();
    }

    public void incSqsReceiveEmpty() {
        sqsReceiveEmptyTotal.increment();
    }

    public void incSqsDeleteOk() {
        sqsDeleteTotal.increment();
    }

    public void incSqsDeleteFail() {
        sqsDeleteFailedTotal.increment();
    }


    public void incRelayPublished() {
        relayPublishedTotal.increment();
    }

    public void incRelayFailed() {
        relayFailedTotal.increment();
    }

    public void recordProcessing(Runnable r) {
        processingTimer.record(r);
    }
}