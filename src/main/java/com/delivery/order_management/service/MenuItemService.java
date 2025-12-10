package com.delivery.order_management.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.order_management.exception.InsufficientStockException;
import com.delivery.order_management.exception.ResourceNotFoundException;
import com.delivery.order_management.model.entity.MenuItem;
import com.delivery.order_management.model.entity.Restaurant;
import com.delivery.order_management.repository.MenuItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;

    /**
     * Get all menu items
     */
    public List<MenuItem> getAllMenuItems() {
        log.info("Fetching all menu items");
        return menuItemRepository.findAll();
    }

    /**
     * Get menu item by ID
     */
    public MenuItem getMenuItemById(Long id) {
        log.info("Fetching menu item with id: {}", id);
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    /**
     * Get menu items by restaurant
     */
    public List<MenuItem> getMenuItemsByRestaurant(Long restaurantId) {
        log.info("Fetching menu items for restaurant: {}", restaurantId);

        // Validate restaurant exists
        restaurantService.getRestaurantById(restaurantId);

        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    /**
     * Get available menu items by restaurant
     */
    public List<MenuItem> getAvailableMenuItems(Long restaurantId) {
        log.info("Fetching available menu items for restaurant: {}", restaurantId);
        return menuItemRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId);
    }

    /**
     * Get menu items by category
     */
    public List<MenuItem> getMenuItemsByCategory(String category) {
        log.info("Fetching menu items in category: {}", category);
        return menuItemRepository.findByCategory(category);
    }

    /**
     * Get menu items by price range
     */
    public List<MenuItem> getMenuItemsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching menu items with price between {} and {}", minPrice, maxPrice);
        return menuItemRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Create new menu item
     */
    @Transactional
    public MenuItem createMenuItem(Long restaurantId, MenuItem menuItem) {
        log.info("Creating new menu item for restaurant: {}", restaurantId);

        // Validate restaurant exists
        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);

        // Set restaurant
        menuItem.setRestaurant(restaurant);

        // Validate price
        if (menuItem.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        return menuItemRepository.save(menuItem);
    }

    /**
     * Update menu item
     */
    @Transactional
    public MenuItem updateMenuItem(Long id, MenuItem menuItemDetails) {
        log.info("Updating menu item with id: {}", id);

        MenuItem menuItem = getMenuItemById(id);

        // Update fields
        menuItem.setName(menuItemDetails.getName());
        menuItem.setDescription(menuItemDetails.getDescription());
        menuItem.setPrice(menuItemDetails.getPrice());
        menuItem.setCategory(menuItemDetails.getCategory());
        menuItem.setIsAvailable(menuItemDetails.getIsAvailable());
        menuItem.setStockQuantity(menuItemDetails.getStockQuantity());

        return menuItemRepository.save(menuItem);
    }

    /**
     * Delete menu item
     */
    @Transactional
    public void deleteMenuItem(Long id) {
        log.info("Deleting menu item with id: {}", id);

        MenuItem menuItem = getMenuItemById(id);
        menuItemRepository.delete(menuItem);
    }

    /**
     * Check if menu item is available with sufficient stock
     */
    public boolean checkAvailability(Long menuItemId, Integer quantity) {
        log.info("Checking availability for menu item: {} with quantity: {}", menuItemId, quantity);
        return menuItemRepository.isAvailableWithStock(menuItemId, quantity);
    }

    /**
     * Decrease stock quantity
     */
    @Transactional
    public void decreaseStock(Long menuItemId, Integer quantity) {
        log.info("Decreasing stock for menu item: {} by quantity: {}", menuItemId, quantity);

        // Check availability first
        if (!checkAvailability(menuItemId, quantity)) {
            throw new InsufficientStockException("Insufficient stock for menu item: " + menuItemId);
        }

        int updated = menuItemRepository.decreaseStock(menuItemId, quantity);

        if (updated == 0) {
            throw new InsufficientStockException("Failed to decrease stock for menu item: " + menuItemId);
        }
    }

    /**
     * Increase stock quantity (for returns/cancellations)
     */
    @Transactional
    public void increaseStock(Long menuItemId, Integer quantity) {
        log.info("Increasing stock for menu item: {} by quantity: {}", menuItemId, quantity);

        MenuItem menuItem = getMenuItemById(menuItemId);
        menuItem.setStockQuantity(menuItem.getStockQuantity() + quantity);
        menuItemRepository.save(menuItem);
    }

    /**
     * Bulk update stock quantities
     */
    @Transactional
    public void bulkUpdateStock(Map<Long, Integer> stockUpdates) {
        log.info("Bulk updating stock for {} items", stockUpdates.size());

        for (Map.Entry<Long, Integer> entry : stockUpdates.entrySet()) {
            Long menuItemId = entry.getKey();
            Integer newQuantity = entry.getValue();

            MenuItem menuItem = getMenuItemById(menuItemId);
            menuItem.setStockQuantity(newQuantity);
            menuItemRepository.save(menuItem);

            log.info("Updated stock for {}: {}", menuItem.getName(), newQuantity);
        }
    }

    /**
     * Get low stock items
     */
    public List<MenuItem> getLowStockItems(Integer threshold) {
        log.info("Fetching items with stock below: {}", threshold);
        return menuItemRepository.findAll().stream()
                .filter(item -> item.getStockQuantity() < threshold)
                .toList();
    }
}