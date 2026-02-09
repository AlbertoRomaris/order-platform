package com.orderplatform.core.application.port;

import java.util.UUID;

public interface OrderProcessor {
    void process(UUID orderId) throws Exception;
}
