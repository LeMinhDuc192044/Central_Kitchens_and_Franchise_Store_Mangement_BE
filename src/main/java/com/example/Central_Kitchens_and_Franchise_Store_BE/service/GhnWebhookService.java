package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Shipment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnWebhookPayload;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.GhnStatusMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnWebhookService {
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository kitchenOrderRepository;
    private final GhnStatusMapper ghnStatusMapper;

    @Transactional
    public void handleWebhook(GhnWebhookPayload payload) {
        String ghnOrderCode = payload.getOrderCode();
        String ghnStatus    = payload.getStatus();

        log.info("Webhook received: orderCode={}, status={}", ghnOrderCode, ghnStatus);

        // Step 1: Find delivery order by GHN order code
        Shipment deliveryOrder = shipmentRepository
                .findByGhnOrderCode(ghnOrderCode)
                .orElseThrow(() -> {
                    log.warn("No delivery order found for GHN code: {}", ghnOrderCode);
                    return new RuntimeException("Delivery order not found: " + ghnOrderCode);
                });

        // Step 2: Update delivery order status
        deliveryOrder.setShipStatus(ghnStatus);
        deliveryOrder.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(deliveryOrder);
        log.info("DeliveryOrder [{}] status updated to: {}", ghnOrderCode, ghnStatus);

        // Step 3: Map GHN status → your OrderStatus and update kitchen order
        Order kitchenOrder = deliveryOrder.getOrderDetail().getOrder();
        if (kitchenOrder != null) {
            OrderStatus newStatus = ghnStatusMapper.map(ghnStatus);
            kitchenOrder.setStatusOrder(newStatus);
            kitchenOrderRepository.save(kitchenOrder);
            log.info("KitchenOrder [{}] status updated to: {}", kitchenOrder.getOrderId(), newStatus);
        }
    }
}
