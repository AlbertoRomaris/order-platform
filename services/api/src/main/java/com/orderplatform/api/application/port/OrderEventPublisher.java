package com.orderplatform.api.application.port;

import java.util.UUID;

public interface OrderEventPublisher {
    void publishOrderCreated(UUID orderId);
}
