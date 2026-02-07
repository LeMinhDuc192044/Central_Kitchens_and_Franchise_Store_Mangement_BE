package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderDetail;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderDetailItem;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderDetailRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.FoodPriceUtil;
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


    // 1. TẠO ORDER MỚI (CÓ KÈM ORDER DETAILS VÀ ITEMS)
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // Tự generate ra ID theo format ORDxxx
        String orderId = orderIdGenerator.generateOrderId();

        // Bước 1: Chuyển từ DTO Request → Entity
        Order order = Order.builder()
                .orderId(orderId)
                .note(request.getNote())
                .statusOrder(OrderStatus.PENDING)
                .storeId(request.getStoreId())
                .orderDate(LocalDate.now())
                .build();

        // Bước 2: Tạo OrderDetails (có thể có nhiều OrderDetail, mỗi OrderDetail chứa nhiều món)
        for (OrderDetailRequest detailRequest : request.getOrderDetails()) {
            OrderDetail orderDetail = buildOrderDetail(order, detailRequest);
            order.addOrderDetail(orderDetail);
        }

        // Bước 3: Lưu vào database (cascade sẽ tự động lưu OrderDetail và OrderDetailItem)
        Order savedOrder = orderRepository.save(order);

        // Đếm tổng số món trong tất cả OrderDetails
        int totalItems = request.getOrderDetails().stream()
                .mapToInt(detail -> detail.getItems().size())
                .sum();

        log.info("Created order {} with {} OrderDetail(s) containing {} total item(s)",
                orderId, request.getOrderDetails().size(), totalItems);

        // Bước 4: Chuyển Entity → DTO Response để trả về
        return mapToResponse(savedOrder);
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

    // 5. Update Order Status
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderUpdateRequest updateRequest) {

        // Tìm order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatusOrder();
        OrderStatus newStatus = updateRequest.getNewStatus();

        // Validate transition
        statusValidator.validateTransition(currentStatus, newStatus);

        // Cập nhật trạng thái
        order.setStatusOrder(newStatus);

        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
            log.info("Order {} status change note: {}", orderId, updateRequest.getNote());
        }

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}", orderId, currentStatus, newStatus);

        return toResponse(savedOrder);
    }

    // 6. CẬP NHẬT PRIORITY LEVEL
    @Transactional
    public OrderResponse updateOrderPriority(String orderId, PriorityUpdateRequest updateRequest) {

        // Bước 1: Tìm order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        Integer currentPriority = order.getPriorityLevel();
        Integer newPriority = updateRequest.getNewPriority();
        OrderStatus currentStatus = order.getStatusOrder();

        // Bước 2: Validate priority change
        priorityValidator.validatePriorityChange(currentStatus, currentPriority, newPriority);

        // Bước 3: Cập nhật priority
        order.setPriorityLevel(newPriority);

        // Bước 4: Tự động chuyển status từ PENDING → IN_PROGRESS (nếu là lần đầu set priority)
        boolean autoTransitioned = false;
        if (priorityValidator.shouldAutoTransitionToInProgress(currentStatus, currentPriority, newPriority)) {
            order.setStatusOrder(OrderStatus.IN_PROGRESS);
            autoTransitioned = true;
            log.info("Auto-transitioning order {} from PENDING to IN_PROGRESS (first priority assignment)",
                    orderId);
        }

        // Bước 5: Log lý do thay đổi nếu có
        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
            log.info("Order {} priority change note: {}", orderId, updateRequest.getNote());
        }

        // Bước 6: Lưu vào database
        Order savedOrder = orderRepository.save(order);

        // Bước 7: Log kết quả
        if (autoTransitioned) {
            log.info("Order {} priority set to {} ({}) and status changed: {} → {}",
                    orderId,
                    newPriority, priorityValidator.getPriorityName(newPriority),
                    currentStatus, OrderStatus.IN_PROGRESS);
        } else {
            log.info("Order {} priority updated: {} ({}) → {} ({}) - Status remains: {}",
                    orderId,
                    currentPriority, priorityValidator.getPriorityName(currentPriority),
                    newPriority, priorityValidator.getPriorityName(newPriority),
                    currentStatus);
        }

        return toResponse(savedOrder);
    }

    // 7. Lấy order by storeID
    public List<OrderResponse> getOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreId(storeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 8. Cancel order
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setNewStatus(OrderStatus.CANCELLED);
        updateRequest.setNote(reason);

        return updateOrderStatus(orderId, updateRequest);
    }

    // 9. LẤY ORDER DETAIL THEO ID (Cách tối ưu với repository)
    @Transactional
    public OrderDetailResponse getOrderDetailById(String orderDetailId) {
        OrderDetail orderDetail = orderDetailRepository.findByOrderDetailId(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order detail not found with id: " + orderDetailId));

        log.info("Retrieved order detail {} with {} items",
                orderDetailId, orderDetail.getOrderDetailItems().size());

        return mapToOrderDetailResponse(orderDetail);
    }


// ==================== HELPER METHODS ====================

    /**
     * Tạo OrderDetail entity từ request (chứa nhiều items)
     */
    private OrderDetail buildOrderDetail(Order order, OrderDetailRequest request) {
        String orderDetailId = IdGeneratorUtil.generateOrderDetailId();

        // Tạo OrderDetail
        OrderDetail orderDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .orderId(order.getOrderId())
                .storeId(order.getStoreId())
                .amount(BigDecimal.ZERO) // Sẽ tính sau
                .note(request.getNote())  // ✅ THÊM NOTE Từ REQUEST
                .build();

        // Tạo các OrderDetailItems và tính tổng amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDetailItemRequest itemRequest : request.getItems()) {
            OrderDetailItem item = buildOrderDetailItem(orderDetail, itemRequest);
            orderDetail.addOrderDetailItem(item);
            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        // Set tổng amount cho OrderDetail
        orderDetail.setAmount(totalAmount);

        return orderDetail;
    }

    /**
     * Tạo OrderDetailItem entity từ request
     */
    private OrderDetailItem buildOrderDetailItem(OrderDetail orderDetail, OrderDetailItemRequest request) {
        String itemId = IdGeneratorUtil.generateOrderDetailItemId();
        BigDecimal unitPrice = FoodPriceUtil.getPrice(request.getFoodItem());
        BigDecimal totalAmount = FoodPriceUtil.calculateAmount(request.getFoodItem(), request.getQuantity());

        return OrderDetailItem.builder()
                .orderDetailItemId(itemId)
                .orderDetailId(orderDetail.getOrderDetailId())
                .foodItem(request.getFoodItem())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * Chuyển Entity → Response DTO (có kèm OrderDetails và Items)
     */
    private OrderResponse mapToResponse(Order order) {
        List<OrderDetailResponse> orderDetailResponses = order.getOrderDetails().stream()
                .map(this::mapToOrderDetailResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .priorityLevel(order.getPriorityLevel())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .orderDetails(orderDetailResponses)
                .build();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .priorityLevel(order.getPriorityLevel())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .build();
    }

    /**
     * Chuyển OrderDetail Entity → Response DTO (có kèm Items)
     */
    private OrderDetailResponse mapToOrderDetailResponse(OrderDetail orderDetail) {
        List<OrderDetailItemResponse> itemResponses = orderDetail.getOrderDetailItems().stream()
                .map(this::mapToOrderDetailItemResponse)
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderDetailId(orderDetail.getOrderDetailId())
                .amount(orderDetail.getAmount())
                .note(orderDetail.getNote())  // ✅ THÊM NOTE Từ ENTITY
                .items(itemResponses)
                .build();
    }

    /**
     * Chuyển OrderDetailItem Entity → Response DTO
     */
    private OrderDetailItemResponse mapToOrderDetailItemResponse(OrderDetailItem item) {
        return OrderDetailItemResponse.builder()
                .foodItem(item.getFoodItem())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalAmount(item.getTotalAmount())
                .orderDetailId(item.getOrderDetailId())
                .build();
    }
}