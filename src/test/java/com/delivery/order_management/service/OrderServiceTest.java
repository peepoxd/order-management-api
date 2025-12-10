package com.delivery.order_management.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.order_management.exception.InsufficientStockException;
import com.delivery.order_management.exception.InvalidOrderException;
import com.delivery.order_management.model.dto.OrderItemRequest;
import com.delivery.order_management.model.entity.MenuItem;
import com.delivery.order_management.model.entity.Restaurant;
import com.delivery.order_management.repository.OrderRepository;
import com.delivery.order_management.util.OrderNumberGenerator;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuItemService menuItemService;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @InjectMocks
    private OrderService orderService;

    private Restaurant testRestaurant;
    private MenuItem testMenuItem;

    @BeforeEach
    void setUp() {
        testRestaurant = Restaurant.builder()
                .id(1L)
                .name("Test Restaurant")
                .isActive(true)
                .build();

        testMenuItem = MenuItem.builder()
                .id(1L)
                .restaurant(testRestaurant)
                .name("Test Item")
                .price(BigDecimal.valueOf(100))
                .isAvailable(true)
                .stockQuantity(10)
                .build();
    }

    @Test
    void shouldThrowExceptionWhenRestaurantNotActive() {
        // Given
        testRestaurant.setIsActive(false);
        when(restaurantService.getRestaurantById(1L)).thenReturn(testRestaurant);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(1L, "Customer", "0812345678",
                "Address", new ArrayList<>(), null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Restaurant is not active");
    }

    @Test
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        when(restaurantService.getRestaurantById(1L)).thenReturn(testRestaurant);
        when(restaurantService.isRestaurantOpen(1L)).thenReturn(true);
        when(menuItemService.getMenuItemById(1L)).thenReturn(testMenuItem);
        when(menuItemService.checkAvailability(1L, 20)).thenReturn(false);

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .menuItemId(1L)
                .quantity(20) // More than available
                .build();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(1L, "Customer", "0812345678",
                "Address", List.of(itemRequest), null))
                .isInstanceOf(InsufficientStockException.class);
    }
}
