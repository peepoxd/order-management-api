package com.delivery.order_management.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.order_management.model.dto.CreateOrderRequest;
import com.delivery.order_management.model.dto.OrderItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .menuItemId(1L)
                .quantity(2)
                .notes("Extra spicy")
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(1L)
                .customerName("John Doe")
                .customerPhone("0898765432")
                .deliveryAddress("123 Test St")
                .items(List.of(itemRequest))
                .notes("Test order")
                .build();

        // When & Then
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    void shouldGetOrderStatistics() throws Exception {
        mockMvc.perform(get("/orders/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").exists())
                .andExpect(jsonPath("$.pendingOrders").exists());
    }
}
