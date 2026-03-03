package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailItemResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderDetail;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderDetailItem;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderInvoice;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodsRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderDetailRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderInvoiceRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.IdGeneratorUtil;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.OrderIdGenerator;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.OrderStatusValidator;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.PriorityLevelValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusValidator statusValidator;
    private final OrderIdGenerator orderIdGenerator;
    private final PriorityLevelValidator priorityValidator;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderInvoiceRepository orderInvoiceRepository;
    private final CentralFoodsRepository centralFoodsRepository;

    // 1. TẠO ORDER MỚI
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String orderId = orderIdGenerator.generateOrderId();

        Order order = Order.builder()
                .orderId(orderId)
                .statusOrder(OrderStatus.PENDING)
                .paymentOption(request.getPaymentOption())
                .storeId(request.getStoreId())
                .orderDate(LocalDate.now())
                .note(request.getNote())
                .build();

        // ✅ Chỉ build 1 OrderDetail duy nhất
        OrderDetail orderDetail = buildOrderDetail(order, request.getOrderDetail());
        order.assignOrderDetail(orderDetail);

        Order savedOrder = orderRepository.save(order);

        // ✅ Lấy trực tiếp, không cần .get(0)
        String orderDetailId = savedOrder.getOrderDetail().getOrderDetailId();

        OrderInvoice invoice = OrderInvoice.builder()
                .orderInvoiceId("INV-" + savedOrder.getOrderId())
                .orderId(orderDetailId)
                .invoiceStatus("PENDING")
                .build();
        orderInvoiceRepository.save(invoice);

        log.info("Created order {} with {} item(s)",
                orderId, request.getOrderDetail().getItems().size());

        return toResponse(savedOrder);
    }

    // 2. LẤY ORDER THEO ID
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    // 3. LẤY TẤT CẢ ORDERS
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 4. XÓA ORDER
    @Transactional
    public void deleteOrder(String orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(orderId);
    }

    // 5. UPDATE ORDER STATUS
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderUpdateRequest updateRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatusOrder();
        OrderStatus newStatus = updateRequest.getNewStatus();
        statusValidator.validateTransition(currentStatus, newStatus);

        order.setStatusOrder(newStatus);

        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
            log.info("Order {} status change note: {}", orderId, updateRequest.getNote());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated: {} → {}", orderId, currentStatus, newStatus);
        return toResponse(savedOrder);
    }

    // 6. CẬP NHẬT PRIORITY LEVEL
    @Transactional
    public OrderResponse updateOrderPriority(String orderId, PriorityUpdateRequest updateRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        Integer currentPriority = order.getPriorityLevel();
        Integer newPriority = updateRequest.getNewPriority();
        OrderStatus currentStatus = order.getStatusOrder();

        priorityValidator.validatePriorityChange(currentStatus, currentPriority, newPriority);
        order.setPriorityLevel(newPriority);

        if (priorityValidator.shouldAutoTransitionToInProgress(currentStatus, currentPriority, newPriority)) {
            order.setStatusOrder(OrderStatus.IN_PROGRESS);
            log.info("Order {} auto-transitioned to IN_PROGRESS", orderId);
        }

        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    // 7. LẤY ORDER THEO STORE ID
    public List<OrderResponse> getOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreId(storeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 8. CANCEL ORDER
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setNewStatus(OrderStatus.CANCELLED);
        updateRequest.setNote(reason);
        return updateOrderStatus(orderId, updateRequest);
    }


    // 11. UPDATE ORDER DETAIL
    @Transactional
    public OrderDetailResponse updateOrderDetail(String orderDetailId, OrderDetailUpdateRequest request) {
        OrderDetail orderDetail = orderDetailRepository.findByOrderDetailId(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order detail not found with id: " + orderDetailId));

        Order order = orderRepository.findById(orderDetail.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order not found with id: " + orderDetail.getOrderId()));

        if (!canUpdateOrderDetail(order.getStatusOrder())) {
            throw new IllegalStateException(
                    "Cannot update order detail. Order status is: " + order.getStatusOrder());
        }


        // ✅ Xóa items cũ và rebuild
        orderDetail.getOrderDetailItems().clear();

        for (OrderDetailItemRequest itemRequest : request.getItems()) {
            OrderDetailItem item = buildOrderDetailItem(orderDetail, itemRequest);
            orderDetail.addOrderDetailItem(item); // ← amount tự động được sync trong helper method
        }

        OrderDetail savedOrderDetail = orderDetailRepository.save(orderDetail);
        log.info("Updated order detail {} - New amount: {}, Total items: {}",
                orderDetailId, savedOrderDetail.getAmount(), request.getItems().size());

        return mapToOrderDetailResponse(savedOrderDetail);
    }

    // 12. LẤY ORDERS PENDING THEO STORE ID
    public List<OrderResponse> getAllOrdersWithPendingStatusByStoreId(String storeId) {
        return orderRepository.findByStoreIdAndStatusOrder(storeId, OrderStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 13. LẤY TẤT CẢ ORDERS PENDING
    public List<OrderResponse> getAllOrdersWithPendingStatus() {
        return orderRepository.findByStatusOrder(OrderStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 14. XÁC NHẬN ORDER
    @Transactional
    public OrderResponse confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order. Current status is: " + order.getStatusOrder());
        }

        order.setStatusOrder(OrderStatus.IN_PROGRESS);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} confirmed: PENDING → IN_PROGRESS", orderId);
        return toResponse(savedOrder);
    }

    // ==================== HELPER METHODS ====================

    private boolean canUpdateOrderDetail(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.IN_PROGRESS;
    }

    private OrderDetail buildOrderDetail(Order order, OrderDetailRequest request) {
        String orderDetailId = IdGeneratorUtil.generateOrderDetailId();

        OrderDetail orderDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .orderId(order.getOrderId())
                // ✅ Bỏ storeId
                .amount(BigDecimal.ZERO)
                .build();

        for (OrderDetailItemRequest itemRequest : request.getItems()) {
            OrderDetailItem item = buildOrderDetailItem(orderDetail, itemRequest);
            orderDetail.addOrderDetailItem(item); // ← amount tự sync trong addOrderDetailItem
        }

        return orderDetail;
    }

    private OrderDetailItem buildOrderDetailItem(OrderDetail orderDetail, OrderDetailItemRequest request) {
        CentralFoods centralFood = centralFoodsRepository.findById(request.getCentralFoodId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy sản phẩm: " + request.getCentralFoodId()));

        BigDecimal unitPrice = BigDecimal.valueOf(centralFood.getUnitPriceFood());
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        return OrderDetailItem.builder()
                .orderDetailItemId(IdGeneratorUtil.generateOrderDetailItemId())
                .orderDetailId(orderDetail.getOrderDetailId())
                .centralFoodId(centralFood.getCentralFoodId())
                .foodName(centralFood.getFoodName())  // snapshot
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)                 // snapshot
                .totalAmount(totalAmount)
                .build();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .priorityLevel(order.getPriorityLevel())
                .paymentOption(order.getPaymentOption())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .note(order.getNote())
                .build();
    }

    private OrderDetailResponse mapToOrderDetailResponse(OrderDetail orderDetail) {
        return OrderDetailResponse.builder()
                .orderDetailId(orderDetail.getOrderDetailId())
                .amount(orderDetail.getAmount())
                .items(orderDetail.getOrderDetailItems().stream()
                        .map(this::mapToOrderDetailItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private OrderDetailItemResponse mapToOrderDetailItemResponse(OrderDetailItem item) {
        return OrderDetailItemResponse.builder()
                .centralFoodId(item.getCentralFoodId())
                .foodName(item.getFoodName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalAmount(item.getTotalAmount())
                .build();
    }
}