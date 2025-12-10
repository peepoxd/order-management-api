package com.delivery.order_management.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.order_management.model.dto.CreateOrderRequest;
import com.delivery.order_management.model.dto.OrderItemResponse;
import com.delivery.order_management.model.dto.OrderResponse;
import com.delivery.order_management.model.dto.OrderStatistics;
import com.delivery.order_management.model.dto.UpdateOrderStatusRequest;
import com.delivery.order_management.model.entity.Order;
import com.delivery.order_management.model.entity.OrderItem;
import com.delivery.order_management.model.entity.OrderStatus;
import com.delivery.order_management.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders with pagination")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Order> orders = orderService.getAllOrders(pageable);
        Page<OrderResponse> response = orders.map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/order-number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        Order order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        List<OrderResponse> response = orders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{phone}")
    @Operation(summary = "Get orders by customer phone")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerPhone(@PathVariable String phone) {
        List<Order> orders = orderService.getOrdersByCustomerPhone(phone);
        List<OrderResponse> response = orders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get orders by restaurant")
    public ResponseEntity<Page<OrderResponse>> getOrdersByRestaurant(
            @PathVariable Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Order> orders = orderService.getOrdersByRestaurant(restaurantId, pageable);
        Page<OrderResponse> response = orders.map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent orders")
    public ResponseEntity<Page<OrderResponse>> getRecentOrders(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<Order> orders = orderService.getRecentOrders(pageable);
        Page<OrderResponse> response = orders.map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get order statistics")
    public ResponseEntity<OrderStatistics> getOrderStatistics() {
        OrderStatistics stats = orderService.getOrderStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    @Operation(summary = "Create new order")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        Order order = orderService.createOrder(
                request.getRestaurantId(),
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getDeliveryAddress(),
                request.getItems(),
                request.getNotes());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(toResponse(order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(toResponse(order));
    }

    // Helper methods
    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .notes(order.getNotes())
                .items(order.getOrderItems().stream()
                        .map(this::toOrderItemResponse)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .menuItemId(orderItem.getMenuItem().getId())
                .menuItemName(orderItem.getMenuItem().getName())
                .quantity(orderItem.getQuantity())
                .priceAtOrder(orderItem.getPriceAtOrder())
                .subtotal(orderItem.getSubtotal())
                .notes(orderItem.getNotes())
                .build();
    }
}
