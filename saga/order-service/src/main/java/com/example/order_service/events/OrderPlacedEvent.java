package com.example.order_service.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String customerId;
    private String restaurantId;
    private BigDecimal amount;
    private LocalDateTime timestamp;

    public OrderPlacedEvent(String orderId, String customerId,
                            String restaurantId, BigDecimal amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
}