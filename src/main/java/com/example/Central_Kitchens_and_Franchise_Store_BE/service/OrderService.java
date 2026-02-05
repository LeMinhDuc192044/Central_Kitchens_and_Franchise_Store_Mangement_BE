package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.PriorityUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.OrderIdGenerator;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.OrderStatusValidator;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.PriorityLevelValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor  // Tự động tạo constructor cho final fields
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusValidator statusValidator;
    private final OrderIdGenerator orderIdGenerator;
    private final PriorityLevelValidator priorityValidator;


    // 1. TẠO ORDER MỚI
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // Tự generate ra ID theo format ORDxxx
        String orderID = orderIdGenerator.generateOrderId();

        // Bước 1: Chuyển từ DTO Request → Entity
        Order order = Order.builder()
                .orderId(orderID)
                .note(request.getNote())
                .statusOrder(OrderStatus.PENDING)
                .storeId(request.getStoreId())
                .orderDate(LocalDate.now())
                .build();

        // Bước 2: Lưu vào database
        Order savedOrder = orderRepository.save(order);

        // Bước 3: Chuyển Entity → DTO Response để trả về
        return mapToResponse(savedOrder);
    }

    // 2. LẤY ORDER THEO ID
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return mapToResponse(order);
    }

    // 3. LẤY TẤT CẢ ORDERS
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)  // Chuyển từng Order → OrderResponse
                .collect(Collectors.toList());
    }

//    // 4. CẬP NHẬT ORDER
//    @Transactional
//    public OrderResponse updateOrder(String orderId, OrderRequest request) {
//        // Bước 1: Tìm order cần update
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        // Bước 2: Cập nhật các field
//        order.setNote(request.getNote());
//        order.setStoreId(request.getStoreId());
//
//        // Bước 3: Lưu lại (JPA tự động update)
//        Order updatedOrder = orderRepository.save(order);
//
//        return mapToResponse(updatedOrder);
//    }

    // 5. XÓA ORDER
    @Transactional
    public void deleteOrder(String orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(orderId);
    }

    //6. Update Order Status
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

        return mapToResponse(savedOrder);
    }

    // 7. CẬP NHẬT PRIORITY LEVEL
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

        return mapToResponse(savedOrder);
    }

    //8. Lấy order by storeID
    public List<OrderResponse> getOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreId(storeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 9. Cancel order
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setNewStatus(OrderStatus.CANCELLED);
        updateRequest.setNote(reason);

        return updateOrderStatus(orderId, updateRequest);
    }


    // Helper method: Chuyển Entity → Response DTO
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .priorityLevel(order.getPriorityLevel())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .build();
    }

}
