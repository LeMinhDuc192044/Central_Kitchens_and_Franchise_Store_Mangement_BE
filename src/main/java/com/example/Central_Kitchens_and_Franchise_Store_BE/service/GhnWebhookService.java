package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.ShipInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Shipment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.InvoiceStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipmentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnWebhookPayload;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.GhnStatusMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.ShipInvoiceRepository;
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
    private final ShipInvoiceRepository shipInvoiceRepository;

    @Transactional
    public void handleWebhook(GhnWebhookPayload payload) {
        String ghnOrderCode = payload.getOrderCode();
        String ghnStatus    = payload.getStatus();

        log.info("Webhook received: orderCode={}, status={}", ghnOrderCode, ghnStatus);

        // Step 1: Find shipment by GHN order code
        Shipment deliveryOrder = shipmentRepository
                .findByGhnOrderCode(ghnOrderCode)
                .orElseThrow(() -> {
                    log.warn("No shipment found for GHN code: {}", ghnOrderCode);
                    return new RuntimeException("Shipment not found: " + ghnOrderCode);
                });

        // Step 2: Map GHN status string → ShipmentStatus enum (dùng mapper, KHÔNG dùng valueOf)
        ShipmentStatus newShipmentStatus;
        OrderStatus newOrderStatus;
        try {
            newShipmentStatus = ghnStatusMapper.toShipmentStatus(ghnStatus); // ✅
            newOrderStatus    = ghnStatusMapper.toOrderStatus(ghnStatus);    // ✅
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized GHN status '{}' for order {} — webhook ignored.", ghnStatus, ghnOrderCode);
            return; // Không throw, tránh GHN retry vô hạn
        }

        // Step 3: Update Shipment
        deliveryOrder.setShipStatus(newShipmentStatus);
        deliveryOrder.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(deliveryOrder);
        log.info("Shipment [{}] status updated to: {}", ghnOrderCode, newShipmentStatus);

        // Step 4: Update Order
        Order kitchenOrder = deliveryOrder.getOrderDetail().getOrder();
        if (kitchenOrder == null) {
            log.warn("Shipment [{}] has no linked Order — skipping order update.", ghnOrderCode);
            return;
        }

        kitchenOrder.setStatusOrder(newOrderStatus);
        kitchenOrderRepository.save(kitchenOrder);
        log.info("Order [{}] status updated to: {}", kitchenOrder.getOrderId(), newOrderStatus);

        // Step 5: Update ShipInvoice
        ShipInvoice shipInvoice = deliveryOrder.getShipInvoice();
        if (shipInvoice == null) {
            log.warn("Shipment [{}] has no ShipInvoice — skipping invoice update.", ghnOrderCode);
            return;
        }

        InvoiceStatus newInvoiceStatus = ghnStatusMapper.mapToInvoiceStatus(newOrderStatus);
        if (newInvoiceStatus != null) {
            shipInvoice.setInvoiceStatus(newInvoiceStatus);
            if (newInvoiceStatus == InvoiceStatus.PAID) {
                shipInvoice.setPaidAt(LocalDateTime.now());
            }
            shipInvoiceRepository.save(shipInvoice);
            log.info("ShipInvoice [{}] updated to: {}", shipInvoice.getShipInvoiceId(), newInvoiceStatus);
        }
    }
}
