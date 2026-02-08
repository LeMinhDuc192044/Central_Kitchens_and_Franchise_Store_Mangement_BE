package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.OrderUpdateRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController  // Kết hợp @Controller + @ResponseBody
@RequestMapping("/orders")  // Base URL
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 1. TẠO ORDER - POST /api/orders
    @PostMapping
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. LẤY ORDER THEO ID - GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by id")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        // @PathVariable: Lấy giá trị từ URL
        // Ví dụ: /api/orders/ORD001 → orderId = "ORD001"
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);  // Status 200 OK
    }

    // 3. LẤY TẤT CẢ ORDERS - GET /api/orders
    @GetMapping
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get all orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 4. LẤY ORDERS THEO STORE - GET /api/orders/store/{storeId}
    @GetMapping("/orders/{storeId}")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get order by store id")
    public ResponseEntity<List<OrderResponse>> getOrdersByStore(@PathVariable String storeId) {
        List<OrderResponse> orders = orderService.getOrdersByStoreId(storeId);
        return ResponseEntity.ok(orders);
    }

    // 5. CẬP NHẬT ORDER - PUT /api/orders/{orderId}
    @PutMapping("/{orderId}")
    @Operation(summary = "Update order")
    public ResponseEntity<OrderResponse> updateOrder(@Valid
            @PathVariable String orderId,
            @RequestBody OrderRequest request) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    // 6. XÓA ORDER - DELETE /api/orders/{orderId}
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Delete order")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();  // Status 204 NO CONTENT
    }


    //Cập nhật trạng thái đơn hàng
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update the status of an order with validation")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,  // ← Đổi từ Long sang String
            @Valid @RequestBody OrderUpdateRequest updateRequest) {

        OrderResponse response = orderService.updateOrderStatus(orderId, updateRequest);
        return ResponseEntity.ok(response);
    }

    //Hủy đơn hàng
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancel an order with reason")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,  // ← Đổi từ Long sang String
            @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }


//    //Xem các luồng đi hợp lệ của status
//    @GetMapping("/{orderId}/available-transitions")
//    @Operation(summary = "Get available status transitions",
//            description = "Get list of statuses that the order can transition to")
//    public ResponseEntity<Set<OrderStatus>> getAvailableTransitions(@PathVariable String orderId) {
//        Set<OrderStatus> transitions = orderService.getAvailableStatusTransitions(orderId);
//        return ResponseEntity.ok(transitions);
//    }
}
