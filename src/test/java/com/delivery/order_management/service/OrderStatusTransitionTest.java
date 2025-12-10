package com.delivery.order_management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.order_management.exception.InvalidOrderException;
import com.delivery.order_management.model.entity.Order;
import com.delivery.order_management.model.entity.OrderStatus;
import com.delivery.order_management.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuItemService menuItemService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldAllowPendingToConfirmed() {
        // Given
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order updated = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Then
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldNotAllowDeliveredToCancelled() {
        // Given
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldNotCancelDeliveredOrder() {
        // Given
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Cannot cancel order");
    }
}
