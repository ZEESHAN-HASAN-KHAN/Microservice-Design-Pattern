package com.example.order_service.service;

import com.example.order_service.model.Order;
import com.example.order_service.events.*;
import com.example.order_service.model.Order.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    // ── SAGA STEP 1 — Place Order ─────────────────────────────
    @Transactional
    public Order placeOrder(String customerId, String restaurantId,
                            BigDecimal amount) {

        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId(customerId)
                .restaurantId(restaurantId)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        log.info("🟡 [SAGA START] Order created: {} | Status: PENDING",
                order.getId());

        kafkaTemplate.send(orderPlacedTopic, order.getId(),
                new OrderPlacedEvent(order.getId(), customerId,
                        restaurantId, amount));

        log.info("📤 Published: order.placed → orderId: {}", order.getId());

        return order;
    }

    // ── SAGA STEP 3 — Payment done, wait for stock ────────────
    @KafkaListener(
            topics = "${kafka.topics.payment-succeeded}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("📥 Received: payment.succeeded → orderId: {}",
                event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.PAYMENT_DONE);
            orderRepository.save(order);
            log.info("✅ Order {} → PAYMENT_DONE", order.getId());
        });
    }

    // ── SAGA STEP 5 — Stock reserved → CONFIRM order ─────────
    @KafkaListener(
            topics = "${kafka.topics.stock-reserved}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void onStockReserved(StockReservedEvent event) {
        log.info("📥 Received: stock.reserved → orderId: {}",
                event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("🎉 [SAGA COMPLETE] Order {} CONFIRMED!", order.getId());
        });
    }

    // ── COMPENSATION — Payment failed ─────────────────────────
    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("📥 Received: payment.failed → orderId: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.warn("🔴 [COMPENSATION] Order {} CANCELLED — reason: {}",
                    order.getId(), event.getReason());
        });
    }

    // ── COMPENSATION — Stock failed ───────────────────────────
    @KafkaListener(
            topics = "${kafka.topics.stock-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void onStockFailed(StockFailedEvent event) {
        log.info("📥 Received: stock.failed → orderId: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.warn("🔴 [COMPENSATION] Order {} CANCELLED — reason: {}",
                    order.getId(), event.getReason());
        });
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}