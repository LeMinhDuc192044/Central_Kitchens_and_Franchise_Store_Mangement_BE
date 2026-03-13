package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentMethodResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentRecordResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.StoreResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreatePaymentRecordRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateStoreRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdatePaymentMethodRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.FranchiseStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/franchise-stores")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RequiredArgsConstructor
public class FranchiseStoreController {

    private final FranchiseStoreService franchiseStoreService;

    @PostMapping
    @Operation(summary = "Create a new franchise store")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StoreResponse> createStore(@RequestBody CreateStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(franchiseStoreService.createStore(request));
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "Get a store information by StoreID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<StoreResponse> getStore(@PathVariable String storeId) {
        return ResponseEntity.ok(franchiseStoreService.getStore(storeId));
    }

    @GetMapping
    @Operation(summary = "Get all stores")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR')")
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        return ResponseEntity.ok(franchiseStoreService.getAllStores());
    }


    @PutMapping("/{storeId}/payment-method")
    @Operation(summary = "change/add franchise store's payment method")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentMethodResponse> updatePaymentMethod(
            @PathVariable String storeId,
            @RequestBody UpdatePaymentMethodRequest request) {

        return ResponseEntity.ok(
                franchiseStoreService.updatePaymentMethod(storeId, request));
    }


    @PostMapping("/payment-records")
    @Operation(summary = "create debt record")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR')")
    public ResponseEntity<PaymentRecordResponse> addPaymentRecord(
            @RequestBody CreatePaymentRecordRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(franchiseStoreService.addPaymentRecord(request));
    }


    @GetMapping("/{storeId}/payment-records")
    @Operation(summary = "get payment record and debt amount")
    //@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<List<PaymentRecordResponse>> getPaymentRecords(
            @PathVariable String storeId) {

        return ResponseEntity.ok(franchiseStoreService.getPaymentRecords(storeId));
    }


    @PatchMapping("/{storeId}/debt-status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update debt status of a store")
    public ResponseEntity<StoreResponse> updateDebtStatus(
            @PathVariable String storeId,
            @RequestParam boolean debtStatus) {
        return ResponseEntity.ok(franchiseStoreService.updateDebtStatus(storeId, debtStatus));
    }


    @PostMapping("/{storeId}/pay-debt-cash")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Pay total store debt by cash - clear all debt records and set deptStatus = false")
    public ResponseEntity<StoreResponse> payDebtByCash(@PathVariable String storeId) {
        return ResponseEntity.ok(franchiseStoreService.payDebtByCash(storeId));
    }

}
