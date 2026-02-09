package com.orderplatform.api.infrastructure.worker;

import com.orderplatform.core.application.port.OrderProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedOrderProcessor implements OrderProcessor {

    @Value("${order.worker.failureProbability:0.0}")
    private double failureProbability;

    @Value("${order.worker.processingDelayMs:500}")
    private long processingDelayMs;

    @Override
    public void process(UUID orderId) throws Exception {
        if (Math.random() < failureProbability) {
            throw new RuntimeException("Simulated failure for testing");
        }
        Thread.sleep(processingDelayMs);
    }
}
