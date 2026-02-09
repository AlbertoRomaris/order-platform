package com.orderplatform.api.config;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderProcessor;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository) {
        return new CreateOrderUseCase(orderRepository);
    }

    @Bean
    public ListDlqEntriesUseCase listDlqEntriesUseCase(OrderDlqRepository dlqRepository) {
        return new ListDlqEntriesUseCase(dlqRepository);
    }

    @Bean
    public ReprocessDlqEntryUseCase reprocessDlqEntryUseCase(OrderDlqRepository dlqRepository,OrderRepository orderRepository) {
        return new ReprocessDlqEntryUseCase(dlqRepository, orderRepository);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderUseCase(orderRepository);
    }

    @Bean
    public ProcessOrderUseCase processOrderUseCase(OrderRepository orderRepository,
                                                   OrderDlqRepository dlqRepository,
                                                   OrderProcessor orderProcessor) {
        return new ProcessOrderUseCase(orderRepository, dlqRepository, orderProcessor);
    }



}
