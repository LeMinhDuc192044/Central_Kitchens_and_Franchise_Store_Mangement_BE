package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.Dto.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.Dto.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController  // Kết hợp @Controller + @ResponseBody
@RequestMapping("/api/orders")  // Base URL
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 1. TẠO ORDER - POST /api/orders
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // @RequestBody: Chuyển JSON từ client → OrderRequest object
        OrderResponse response = orderService.createOrder(request);

        // Trả về response với status 201 CREATED
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. LẤY ORDER THEO ID - GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        // @PathVariable: Lấy giá trị từ URL
        // Ví dụ: /api/orders/ORD001 → orderId = "ORD001"
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);  // Status 200 OK
    }

    // 3. LẤY TẤT CẢ ORDERS - GET /api/orders
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 4. LẤY ORDERS THEO STORE - GET /api/orders/store/{storeId}
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStore(@PathVariable String storeId) {
        List<OrderResponse> orders = orderService.getOrdersByStoreId(storeId);
        return ResponseEntity.ok(orders);
    }

    // 5. CẬP NHẬT ORDER - PUT /api/orders/{orderId}
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable String orderId,
            @RequestBody OrderRequest request) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    // 6. XÓA ORDER - DELETE /api/orders/{orderId}
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();  // Status 204 NO CONTENT
    }
}
