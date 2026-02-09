package com.orderplatform.worker.config;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderProcessor;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.application.usecase.ProcessOrderUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProcessOrderUseCase processOrderUseCase(OrderRepository orderRepository,
                                                   OrderDlqRepository dlqRepository,
                                                   OrderProcessor orderProcessor) {
        return new ProcessOrderUseCase(orderRepository, dlqRepository, orderProcessor);
    }
}
