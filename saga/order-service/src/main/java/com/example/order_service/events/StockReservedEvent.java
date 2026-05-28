package com.example.order_service.events;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockReservedEvent {
    private String orderId;
    private String restaurantId;
}