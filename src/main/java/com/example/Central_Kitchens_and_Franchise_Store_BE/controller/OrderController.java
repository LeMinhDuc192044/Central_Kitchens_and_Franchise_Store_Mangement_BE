package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ApiResult;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderInvoiceService;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/orders")
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderInvoiceService orderInvoiceService;

    // 1. TẠO ORDER
    @PostMapping
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'SUPPLY_COORDINATOR', 'CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    // 2. LẤY ORDER THEO ID
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'SUPPLY_COORDINATOR', 'CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get order by order id")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {

        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);  // Status 200 OK
    }

    // 3. LẤY TẤT CẢ ORDERS
    @GetMapping
    @PreAuthorize("hasAnyRole('CENTRAL_KITCHEN_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get all orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 4. LẤY ORDERS THEO STORE ID
    @GetMapping("/orders/{storeId}")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF','SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get all orders by store id")
    public ResponseEntity<List<OrderResponse>> getOrdersByStore(@PathVariable String storeId) {
        List<OrderResponse> orders = orderService.getOrdersByStoreId(storeId);
        return ResponseEntity.ok(orders);
    }


    //5. Update order's status
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR','CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update the status of an order with validation")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderUpdateRequest updateRequest) {

        OrderResponse response = orderService.updateOrderStatus(orderId, updateRequest);
        return ResponseEntity.ok(response);
    }

    //6. Cancel order
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR','CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancel an order with reason")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }

    //7. Update priority level
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
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

    @GetMapping("/{orderId}/detail")
    @Operation(
            summary = "Get order detail by OrderId"
    )
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailByOrderId(orderId));
    }

    // 11. LẤY ORDERS CÓ STATUS = PENDING THEO STORE ID
    @GetMapping("/store/{storeId}/pending")
    @Operation(
            summary = "Get all pending orders by store ID",
            description = "Retrieve all orders with PENDING status for a specific store"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved pending orders",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            )
    })
    public ResponseEntity<List<OrderResponse>> getPendingOrdersByStore(@PathVariable String storeId) {
        List<OrderResponse> orders = orderService.getAllOrdersWithPendingStatusByStoreId(storeId);
        return ResponseEntity.ok(orders);
    }

    // 12. LẤY TẤT CẢ ORDERS CÓ STATUS = PENDING
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR','FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Get all pending orders",
            description = "Retrieve all orders with PENDING status across all stores"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved all pending orders",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            )
    })
    public ResponseEntity<List<OrderResponse>> getAllPendingOrders() {
        List<OrderResponse> orders = orderService.getAllOrdersWithPendingStatus();
        return ResponseEntity.ok(orders);
    }

    // 14. XÁC NHẬN ORDER
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Confirm order",
            description = "Confirm a PENDING order and change status to IN_PROGRESS"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order confirmed successfully, status changed to IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Order is not in PENDING status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderId,@RequestParam Integer priorityLevel) {
        OrderResponse response = orderService.confirmOrder(orderId,priorityLevel);
        return ResponseEntity.ok(response);
    }

    // ── GET Invoice theo OrderID
    @GetMapping("/{orderId}/invoice")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Get invoice by order ID",
            description = "Retrieve the invoice associated with a specific order"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderInvoiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderInvoiceResponse> getInvoiceByOrderId(@PathVariable String orderId) {
        OrderInvoiceResponse response = orderInvoiceService.getInvoiceByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cash/{orderId}")
    @Operation(summary = "Pay by cash - auto set payment status to SUCCESS")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<OrderResponse>> payByCash(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResult.success("Thanh toán tiền mặt thành công",
                orderService.payByCash(orderId)));
    }


    @PatchMapping("/change-method/{orderId}")
    @Operation(summary = "Change payment method (CASH/CREDIT) - only if not paid yet")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<OrderResponse>> changePaymentMethod(
            @PathVariable String orderId,
            @RequestParam PaymentMethod newMethod) {
        return ResponseEntity.ok(ApiResult.success("Đổi phương thức thanh toán thành công",
                orderService.changePaymentMethod(orderId, newMethod)));
    }

    @PatchMapping("/change-option/{orderId}")
    @Operation(summary = "Change payment option (PAY_AFTER_ORDER/PAY_AT_THE_END_OF_MONTH) - only if not paid yet")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResult<OrderResponse>> changePaymentOption(
            @PathVariable String orderId,
            @RequestParam PaymentOption newOption) {
        return ResponseEntity.ok(ApiResult.success("Đổi payment option thành công",
                orderService.changePaymentOption(orderId, newOption)));
    }

    // GET ALL CANCELLED ORDERS
    @GetMapping("/cancelled")
    @Operation(summary = "Get all orders having cancel status")
    public ResponseEntity<List<OrderResponse>> getAllCancelledOrders() {
        return ResponseEntity.ok(orderService.getAllCancelledOrders());
    }

    // GET CANCELLED ORDERS BY STORE ID
    @GetMapping("/cancelled/store/{storeId}")
    @Operation(summary = "Get all orders having cancel status by store id")
    public ResponseEntity<List<OrderResponse>> getCancelledOrdersByStoreId(@PathVariable String storeId) {
        return ResponseEntity.ok(orderService.getCancelledOrdersByStoreId(storeId));
    }


}
