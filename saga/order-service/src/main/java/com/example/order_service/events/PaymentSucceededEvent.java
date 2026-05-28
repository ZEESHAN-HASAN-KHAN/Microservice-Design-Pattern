package com.example.order_service.events;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentSucceededEvent {
    private String orderId;
    private String paymentId;
    private String txnId;
}