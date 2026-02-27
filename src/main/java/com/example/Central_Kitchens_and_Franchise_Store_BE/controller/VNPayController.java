package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ApiResult;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CreatePaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResultResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Payment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.PaymentRecord;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.PaymentRecordRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.PaymentRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payment")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/create-by-order/{orderId}")
    @Operation(summary = "Create payment by order id, auto calculate total amount")
    public ResponseEntity<ApiResult<CreatePaymentResponse>> createPaymentByOrder(
            @PathVariable String orderId,
            HttpServletRequest request) {
        CreatePaymentResponse response = vnPayService.createPaymentUrlByOrder(orderId, request);
        return ResponseEntity.ok(ApiResult.success("Tạo URL thanh toán thành công", response));
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay callback after payment")
    public ResponseEntity<ApiResult<PaymentResultResponse>> vnPayReturn(
            HttpServletRequest request) {
        PaymentResultResponse response = vnPayService.processVNPayReturn(request);
        return ResponseEntity.ok(ApiResult.success("Xử lý kết quả thanh toán", response));
    }

    @GetMapping("/{txnRef}")
    @Operation(summary = "Get payment by txnRef")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<PaymentResultResponse>> getPayment(@PathVariable String txnRef) {
        return ResponseEntity.ok(ApiResult.success(vnPayService.getPaymentByTxnRef(txnRef)));
    }

    //get all record
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get all payment, filter by status payment")
    public ResponseEntity<ApiResult<List<PaymentResponse>>> getAllPayments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResult.success(vnPayService.getAllPayments(status)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order id")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<List<PaymentResponse>>> getPaymentsByOrderId(
            @PathVariable String orderId) {
        return ResponseEntity.ok(ApiResult.success(vnPayService.getPaymentsByOrderId(orderId)));
    }



}
