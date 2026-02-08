package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.OrderUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.PriorityUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.response.OrderDetailResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.response.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 1. TẠO ORDER
    @PostMapping
    @Operation(summary = "Create order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. LẤY ORDER THEO ID
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by order id")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {

        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);  // Status 200 OK
    }

    // 3. LẤY TẤT CẢ ORDERS
    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 4. LẤY ORDERS THEO STORE ID
    @GetMapping("/orders/{storeId}")
    @Operation(summary = "Get order by store id")
    public ResponseEntity<List<OrderResponse>> getOrdersByStore(@PathVariable String storeId) {
        List<OrderResponse> orders = orderService.getOrdersByStoreId(storeId);
        return ResponseEntity.ok(orders);
    }

//    // 5. CẬP NHẬT ORDER
//    @PutMapping("/{orderId}")
//    @Operation(summary = "Update order")
//    public ResponseEntity<OrderResponse> updateOrder(@Valid
//            @PathVariable String orderId,
//            @RequestBody OrderRequest request) {
//        OrderResponse response = orderService.updateOrder(orderId, request);
//        return ResponseEntity.ok(response);
//    }

    // 6. XÓA ORDER - DELETE /api/orders/{orderId}
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete order")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();  // Status 204 NO CONTENT
    }


    //7. Update order's status
    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order with validation")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderUpdateRequest updateRequest) {

        OrderResponse response = orderService.updateOrderStatus(orderId, updateRequest);
        return ResponseEntity.ok(response);
    }

    //8. Cancel order
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order with reason")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }

    //9. Update priority level
    @Operation(
            summary = "Update order priority",
            description = "Update the priority level of an order (1=HIGH, 2=MEDIUM, 3=LOW). " +
                    "⚠️ Note: When setting priority for the first time (null → value) and status is PENDING, " +
                    "the status will automatically transition to IN_PROGRESS."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Priority updated successfully. Status may auto-transition from PENDING to IN_PROGRESS if this is the first priority assignment.",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid priority level (must be 1-3) or same as current priority",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot change priority for order status (e.g., CANCELLED, COOKING_DONE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{orderId}/priority")
    public ResponseEntity<OrderResponse> updateOrderPriority(
            @PathVariable String orderId,
            @Valid @RequestBody PriorityUpdateRequest request) {
        OrderResponse response = orderService.updateOrderPriority(orderId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order-details/{orderDetailId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetailById(
            @PathVariable String orderDetailId) {
        OrderDetailResponse response = orderService.getOrderDetailById(orderDetailId);
        return ResponseEntity.ok(response);
    }

}
