package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.OrderUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.OrderStatusValidator;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor  // Tự động tạo constructor cho final fields
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusValidator statusValidator;


    // 1. TẠO ORDER MỚI
    @Transactional  // Đảm bảo transaction, rollback nếu có lỗi
    public OrderResponse createOrder(OrderRequest request) {

        // Bước 1: Chuyển từ DTO Request → Entity
        Order order = Order.builder()
                .orderId(request.getOrderId())
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
        // Tìm trong database, ném exception nếu không tìm thấy
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

    // 4. CẬP NHẬT ORDER
    @Transactional
    public OrderResponse updateOrder(String orderId, OrderRequest request) {
        // Bước 1: Tìm order cần update
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Bước 2: Cập nhật các field
        order.setNote(request.getNote());
        order.setStoreId(request.getStoreId());

        // Bước 3: Lưu lại (JPA tự động update)
        Order updatedOrder = orderRepository.save(order);

        return mapToResponse(updatedOrder);
    }

    // 5. XÓA ORDER
    @Transactional
    public void deleteOrder(String orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(orderId);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderUpdateRequest updateRequest) {
        log.info("Updating order {} to status {}", orderId, updateRequest.getNewStatus());

        // Tìm order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatusOrder();  // ← Sửa thành getStatusOrder()
        OrderStatus newStatus = updateRequest.getNewStatus();

        // Validate transition
        statusValidator.validateTransition(currentStatus, newStatus);

        // Cập nhật trạng thái
        order.setStatusOrder(newStatus);  // ← Sửa thành setStatusOrder()
        // Note: Order entity của bạn không có updatedAt field, bỏ dòng này nếu không cần

        // Lưu note nếu có (giả sử Order entity có trường note hoặc history)
        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
            // Có thể lưu vào history hoặc note field
            // order.addStatusHistory(currentStatus, newStatus, updateRequest.getNote());
            log.info("Order {} status change note: {}", orderId, updateRequest.getNote());
        }

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}", orderId, currentStatus, newStatus);

        return mapToResponse(savedOrder);
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

    public List<OrderResponse> getOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreId(storeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderUpdateRequest updateRequest = new OrderUpdateRequest();
        updateRequest.setNewStatus(OrderStatus.CANCELLED);
        updateRequest.setNote(reason);

        return updateOrderStatus(orderId, updateRequest);
    }

//    public Set<OrderStatus> getAvailableStatusTransitions(String orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
//
//        return statusValidator.getAllowedTransitions(order.getStatusOrder());
//    }
}
