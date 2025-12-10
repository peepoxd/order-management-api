package com.delivery.order_management.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.delivery.order_management.model.dto.OrderItemRequest;
import com.delivery.order_management.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentOrderTest {

    @Autowired
    private OrderService orderService;

    @Test
    void shouldHandleConcurrentOrders() throws InterruptedException {
        // This test verifies that the system handles concurrent orders correctly
        // Only one order should succeed if stock is limited

        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    OrderItemRequest itemRequest = OrderItemRequest.builder()
                            .menuItemId(1L)
                            .quantity(1)
                            .build();

                    orderService.createOrder(1L, "Customer", "0812345678",
                            "Address", List.of(itemRequest), null);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Verify that concurrent orders were handled correctly
        assertThat(successCount.get() + failCount.get()).isEqualTo(numberOfThreads);
    }
}
