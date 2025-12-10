package com.delivery.order_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatistics {
    private Long totalOrders;
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long preparingOrders;
    private Long readyForPickupOrders;
    private Long outForDeliveryOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
}
