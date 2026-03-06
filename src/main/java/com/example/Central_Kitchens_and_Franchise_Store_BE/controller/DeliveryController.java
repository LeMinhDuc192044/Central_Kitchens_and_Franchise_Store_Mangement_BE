package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
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

    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateDeliveryOrderRequest request) {
        Map<String, Object> result = ghnService.createOrder(request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/shipments/{shipmentId}/sync-status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ShipmentStatusUpdateResponse> syncShipmentStatus(
            @PathVariable String shipmentId) {
        return ResponseEntity.ok(ghnService.updateShipmentStatus(shipmentId));
    }

    @GetMapping("/track/{orderCode}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode) {
        Map<String, Object> result = ghnService.trackOrder(orderCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/calculate-fee/{orderCode}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> calculateFeeFromOrder(@PathVariable String orderCode) {
        Map<String, Object> result = ghnService.calculateFeeFromOrder(orderCode);
        return ResponseEntity.ok(result);
    }
}
