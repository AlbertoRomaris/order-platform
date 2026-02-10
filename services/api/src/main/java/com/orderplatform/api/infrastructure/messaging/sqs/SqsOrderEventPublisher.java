package com.orderplatform.api.infrastructure.messaging.sqs;

import com.orderplatform.core.application.port.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "order.events.mode", havingValue = "sqs")
public class SqsOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderEventPublisher.class);

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsOrderEventPublisher(
            SqsClient sqsClient,
            @Value("${aws.sqs.queueUrl}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishOrderCreated(UUID orderId) {
        String body = "{\"orderId\":\"" + orderId + "\"}";

        String correlationId = org.slf4j.MDC.get("correlationId");

        SendMessageRequest.Builder req = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body);

        if (correlationId != null && !correlationId.isBlank()) {
            req.messageAttributes(java.util.Map.of(
                    "correlationId",
                    software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(correlationId)
                            .build()
            ));
        }

        sqsClient.sendMessage(req.build());

        log.info("Published OrderCreated to SQS. orderId={}", orderId);
    }

}
