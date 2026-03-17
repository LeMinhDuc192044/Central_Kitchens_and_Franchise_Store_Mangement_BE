package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ApiResult;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CreatePaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResultResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@Slf4j
@RestController
@RequestMapping("/payment")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;


    @PostMapping("/create-by-order/{orderId}")
    @Operation(summary = "Create payment by order id, auto calculate total amount")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<CreatePaymentResponse>> createPaymentByOrder(
            @PathVariable String orderId,
            HttpServletRequest request) {
        CreatePaymentResponse response = vnPayService.createPaymentUrlByOrder(orderId, request);
        return ResponseEntity.ok(ApiResult.success("Tạo URL thanh toán thành công", response));
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay callback after payment")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public void vnPayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1) Vẫn xử lý nghiệp vụ (update payment/order/invoice)
        vnPayService.processVNPayReturn(request);

        // 2) Redirect về frontend (không trả JSON)
        String query = request.getQueryString(); // giữ nguyên vnp_ResponseCode, vnp_TxnRef, ...
        String frontendUrl = "http://localhost:5173/payment/vnpay-return"
                + (query != null ? "?" + query : "");

        response.sendRedirect(frontendUrl);
    }

    @GetMapping("/{txnRef}")
    @Operation(summary = "Get payment by txnRef")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<PaymentResultResponse>> getPayment(@PathVariable String txnRef) {
        return ResponseEntity.ok(ApiResult.success(vnPayService.getPaymentByTxnRef(txnRef)));
    }



    @PostMapping("/debt/{storeId}")
    @Operation(summary = "Create link payment for store's total debt ")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','MANAGER', 'ADMIN')")
    public ResponseEntity<CreatePaymentResponse> createDebtPayment(
            @PathVariable String storeId,
            HttpServletRequest request) {
        return ResponseEntity.ok(vnPayService.createDebtPaymentByStore(storeId, request));
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResultResponse> refund(
            @PathVariable String orderId,
            HttpServletRequest request) {
        return ResponseEntity.ok(vnPayService.refundPayment(orderId, request));
    }





}
