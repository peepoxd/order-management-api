package com.delivery.order_management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.order_management.exception.InsufficientStockException;
import com.delivery.order_management.model.entity.MenuItem;
import com.delivery.order_management.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class InventoryManagementTest {
    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private MenuItemService menuItemService;

    private MenuItem testMenuItem;

    @BeforeEach
    void setUp() {
        testMenuItem = MenuItem.builder()
                .id(1L)
                .name("Test Item")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .isAvailable(true)
                .build();
    }

    @Test
    void shouldDecreaseStockSuccessfully() {
        // Given
        when(menuItemRepository.isAvailableWithStock(1L, 5)).thenReturn(true);
        when(menuItemRepository.decreaseStock(1L, 5)).thenReturn(1);

        // When & Then
        assertThatCode(() -> menuItemService.decreaseStock(1L, 5))
                .doesNotThrowAnyException();

        verify(menuItemRepository).decreaseStock(1L, 5);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        when(menuItemRepository.isAvailableWithStock(1L, 20)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> menuItemService.decreaseStock(1L, 20))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void shouldIncreaseStockSuccessfully() {
        // Given
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(testMenuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(testMenuItem);

        // When
        menuItemService.increaseStock(1L, 5);

        // Then
        assertThat(testMenuItem.getStockQuantity()).isEqualTo(15);
        verify(menuItemRepository).save(testMenuItem);
    }
}