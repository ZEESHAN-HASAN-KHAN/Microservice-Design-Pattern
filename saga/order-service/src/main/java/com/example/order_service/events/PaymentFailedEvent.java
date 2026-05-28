package com.example.order_service.events;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentFailedEvent {
    private String orderId;
    private String reason;
}