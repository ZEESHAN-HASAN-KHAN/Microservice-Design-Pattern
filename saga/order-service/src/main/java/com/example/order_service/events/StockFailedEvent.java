package com.example.order_service.events;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockFailedEvent {
    private String orderId;
    private String reason;
}