package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.GhnService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/delivery")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery Order")
public class DeliveryController {

    private final GhnService ghnService;

    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateDeliveryOrderRequest request) {
        Map<String, Object> result = ghnService.createOrder(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/track/{orderCode}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode) {
        Map<String, Object> result = ghnService.trackOrder(orderCode);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/calculate-fee")
    public ResponseEntity<?> calculateFee(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = ghnService.calculateFee(
                (int) request.get("from_district_id"),
                (String) request.get("from_ward_code"),
                (int) request.get("to_district_id"),
                (String) request.get("to_ward_code"),
                (int) request.get("weight"),
                (int) request.get("service_type_id")
        );
        return ResponseEntity.ok(result);
    }
}
