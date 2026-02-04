package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController  // Kết hợp @Controller + @ResponseBody
@RequestMapping("/orders")  // Base URL
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 1. TẠO ORDER - POST /api/orders
    @PostMapping
    @Operation(summary = "Create order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        // @RequestBody: Chuyển JSON từ client → OrderRequest object
        OrderResponse response = orderService.createOrder(request);

        // Trả về response với status 201 CREATED
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
    @Operation(summary = "Get all orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 4. LẤY ORDERS THEO STORE - GET /api/orders/store/{storeId}
    @GetMapping("/orders/{storeId}")
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
    @Operation(summary = "Delete order")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();  // Status 204 NO CONTENT
    }
}
