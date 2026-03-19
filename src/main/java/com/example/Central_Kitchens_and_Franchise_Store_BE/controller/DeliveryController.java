package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ShipInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.InvoiceStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipmentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.DeliveryTimeResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipmentInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipmentStatusUpdateResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.GhnService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/delivery")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery Order")
public class DeliveryController {

    private final GhnService ghnService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ShipmentInfo>> getAllShipments() {
        List<ShipmentInfo> shipments = ghnService.getAllShipments();
        return ResponseEntity.ok(shipments);
    }

    @PostMapping("/scheduler/sync-now")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> triggerSyncNow() {
        ghnService.syncShipmentStatuses();
        return ResponseEntity.ok(Map.of(
                "message", "Sync triggered successfully",
                "triggeredAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/expected-delivery-time/from-shop")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")

    public ResponseEntity<DeliveryTimeResponse> getDeliveryTimeFromShop(
            @RequestParam String storeId,
            @RequestParam(required = false) LocalDate date ) {
        return ResponseEntity.ok(
                ghnService.getExpectedDeliveryTimeFromCentralKitchen(
                        storeId, date));
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateDeliveryOrderRequest request) {
        Map<String, Object> result = ghnService.createOrder(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shipments/store/{storeId}")
    public ResponseEntity<List<ShipmentInfo>> getAllShipmentsByStore(
            @PathVariable String storeId,
            @RequestParam(required = false) ShipmentStatus status) {
        return ResponseEntity.ok(
                ghnService.getAllShipmentsByStoreId(storeId, status));
    }

    @PutMapping("/shipments/{shipmentId}/sync-status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ShipmentStatusUpdateResponse> syncShipmentStatus(
            @PathVariable String shipmentId) {
        return ResponseEntity.ok(ghnService.updateShipmentStatus(shipmentId));
    }

    @GetMapping("/track/{orderCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode) {
        Map<String, Object> result = ghnService.trackOrder(orderCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/calculate-fee/{orderCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> calculateFeeFromOrder(@PathVariable String orderCode) {
        Map<String, Object> result = ghnService.calculateFeeFromOrder(orderCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ShipInvoiceResponse>> getAllInvoices(
            @RequestParam(required = false) InvoiceStatus status) {
        List<ShipInvoiceResponse> invoices = ghnService.getAllInvoices(status);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoices/{shipInvoiceId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ShipInvoiceResponse> getInvoiceById(
            @PathVariable String shipInvoiceId) {
        ShipInvoiceResponse invoice = ghnService.getInvoiceById(shipInvoiceId);
        return ResponseEntity.ok(invoice);
    }
}
