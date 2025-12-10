package com.delivery.order_management.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.order_management.exception.InsufficientStockException;
import com.delivery.order_management.exception.InvalidOrderException;
import com.delivery.order_management.exception.ResourceNotFoundException;
import com.delivery.order_management.exception.RestaurantClosedException;
import com.delivery.order_management.model.dto.OrderItemRequest;
import com.delivery.order_management.model.dto.OrderStatistics;
import com.delivery.order_management.model.entity.MenuItem;
import com.delivery.order_management.model.entity.Order;
import com.delivery.order_management.model.entity.OrderItem;
import com.delivery.order_management.model.entity.OrderStatus;
import com.delivery.order_management.model.entity.Restaurant;
import com.delivery.order_management.repository.OrderRepository;
import com.delivery.order_management.util.OrderNumberGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;
    private final OrderNumberGenerator orderNumberGenerator;

    /**
     * Get all orders with pagination
     */
    public Page<Order> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders with pagination");
        return orderRepository.findAll(pageable);
    }

    /**
     * Get order by ID
     */
    public Order getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    /**
     * Get order by order number
     */
    public Order getOrderByOrderNumber(String orderNumber) {
        log.info("Fetching order with order number: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
    }

    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.info("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    /**
     * Get orders by customer phone
     */
    public List<Order> getOrdersByCustomerPhone(String customerPhone) {
        log.info("Fetching orders for customer phone: {}", customerPhone);
        return orderRepository.findByCustomerPhone(customerPhone);
    }

    /**
     * Get orders by restaurant
     */
    public Page<Order> getOrdersByRestaurant(Long restaurantId, Pageable pageable) {
        log.info("Fetching orders for restaurant: {}", restaurantId);
        return orderRepository.findByRestaurantId(restaurantId, pageable);
    }

    /**
     * Create new order - MAIN BUSINESS LOGIC
     */
    @Transactional
    public Order createOrder(Long restaurantId, String customerName, String customerPhone,
            String deliveryAddress, List<OrderItemRequest> itemRequests, String notes) {

        log.info("Creating order for restaurant: {}, customer: {}", restaurantId, customerName);

        // 1. Validate restaurant exists and is active
        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
        if (!restaurant.getIsActive()) {
            throw new InvalidOrderException("Restaurant is not active");
        }

        // 2. Check if restaurant is open (optional - can be commented for testing)
        if (!restaurantService.isRestaurantOpen(restaurantId)) {
            throw new RestaurantClosedException("Restaurant is currently closed");
        }

        // 3. Validate order items are not empty
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        // 4. Build order
        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .restaurant(restaurant)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .deliveryAddress(deliveryAddress)
                .status(OrderStatus.PENDING)
                .notes(notes)
                .orderItems(new ArrayList<>())
                .build();

        // 5. Process order items
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            // Get menu item
            MenuItem menuItem = menuItemService.getMenuItemById(itemRequest.getMenuItemId());

            // Validate menu item belongs to the same restaurant
            if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
                throw new InvalidOrderException(
                        String.format("Menu item %d does not belong to restaurant %d",
                                itemRequest.getMenuItemId(), restaurantId));
            }

            // Check availability and stock
            if (!menuItem.getIsAvailable()) {
                throw new InvalidOrderException("Menu item is not available: " + menuItem.getName());
            }

            if (!menuItemService.checkAvailability(menuItem.getId(), itemRequest.getQuantity())) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                                menuItem.getName(), menuItem.getStockQuantity(), itemRequest.getQuantity()));
            }

            // Calculate subtotal
            BigDecimal priceAtOrder = menuItem.getPrice();
            BigDecimal subtotal = priceAtOrder.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(itemRequest.getQuantity())
                    .priceAtOrder(priceAtOrder)
                    .subtotal(subtotal)
                    .notes(itemRequest.getNotes())
                    .build();

            order.getOrderItems().add(orderItem);
            totalAmount = totalAmount.add(subtotal);

            // Decrease stock
            menuItemService.decreaseStock(menuItem.getId(), itemRequest.getQuantity());

            log.info("Added item to order: {} x {} = {}",
                    menuItem.getName(), itemRequest.getQuantity(), subtotal);
        }

        // 6. Set total amount
        order.setTotalAmount(totalAmount);

        // 7. Save order
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {} with total amount: {}",
                savedOrder.getOrderNumber(), totalAmount);

        // TODO: Day 4 - Send order event to RabbitMQ

        return savedOrder;
    }

    /**
     * Update order status
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} to status: {}", orderId, newStatus);

        Order order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // If cancelling, restore inventory
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            restoreInventory(order);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);

        // TODO: Day 4 - Send status update event to RabbitMQ

        return updatedOrder;
    }

    /**
     * Cancel order
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        log.info("Cancelling order: {}", orderId);

        Order order = getOrderById(orderId);

        // Only allow cancellation of pending or confirmed orders
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException(
                    "Cannot cancel order with status: " + order.getStatus());
        }

        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Define valid transitions
        boolean isValid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELLED;
            case PREPARING -> to == OrderStatus.READY_FOR_PICKUP || to == OrderStatus.CANCELLED;
            case READY_FOR_PICKUP -> to == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };

        if (!isValid) {
            throw new InvalidOrderException(
                    String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    /**
     * Restore inventory when order is cancelled
     */
    private void restoreInventory(Order order) {
        log.info("Restoring inventory for cancelled order: {}", order.getOrderNumber());

        for (OrderItem item : order.getOrderItems()) {
            menuItemService.increaseStock(item.getMenuItem().getId(), item.getQuantity());
            log.info("Restored {} units of {}",
                    item.getQuantity(), item.getMenuItem().getName());
        }
    }

    /**
     * Get order statistics
     */
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    /**
     * Get recent orders
     */
    public Page<Order> getRecentOrders(Pageable pageable) {
        log.info("Fetching recent orders");
        return orderRepository.findRecentOrders(pageable);
    }

    /**
     * Get orders by date range
     */
    public List<Order> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        log.info("Fetching orders between {} and {}", start, end);
        return orderRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * Get orders above certain amount
     */
    public List<Order> getOrdersAboveAmount(BigDecimal amount) {
        log.info("Fetching orders above amount: {}", amount);
        return orderRepository.findOrdersAboveAmount(amount);
    }

    /**
     * Get order summary statistics
     */
    public OrderStatistics getOrderStatistics() {
        log.info("Calculating order statistics");

        long pending = countOrdersByStatus(OrderStatus.PENDING);
        long confirmed = countOrdersByStatus(OrderStatus.CONFIRMED);
        long preparing = countOrdersByStatus(OrderStatus.PREPARING);
        long readyForPickup = countOrdersByStatus(OrderStatus.READY_FOR_PICKUP);
        long outForDelivery = countOrdersByStatus(OrderStatus.OUT_FOR_DELIVERY);
        long delivered = countOrdersByStatus(OrderStatus.DELIVERED);
        long cancelled = countOrdersByStatus(OrderStatus.CANCELLED);

        long total = pending + confirmed + preparing + readyForPickup +
                outForDelivery + delivered + cancelled;

        return OrderStatistics.builder()
                .totalOrders(total)
                .pendingOrders(pending)
                .confirmedOrders(confirmed)
                .preparingOrders(preparing)
                .readyForPickupOrders(readyForPickup)
                .outForDeliveryOrders(outForDelivery)
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .build();
    }
}
