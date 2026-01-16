package com.orderplatform.api.infrastructure.messaging;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class InMemoryOrderQueue {

    private final BlockingQueue<UUID> queue = new LinkedBlockingQueue<>();

    public void publish(UUID orderId) {
        queue.offer(orderId);
    }

    public UUID take() throws InterruptedException {
        return queue.take();
    }
}
