package com.orderplatform.api.config;

import com.orderplatform.core.application.port.OrderDlqRepository;
import com.orderplatform.core.application.port.OrderRepository;
import com.orderplatform.core.application.usecase.CreateOrderUseCase;
import com.orderplatform.core.application.usecase.GetOrderUseCase;
import com.orderplatform.core.application.usecase.ListDlqEntriesUseCase;
import com.orderplatform.core.application.usecase.ReprocessDlqEntryUseCase;
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


}
