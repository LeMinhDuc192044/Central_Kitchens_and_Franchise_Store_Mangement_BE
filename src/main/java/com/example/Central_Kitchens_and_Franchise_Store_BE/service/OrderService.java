package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailItemResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    private final FranchiseStoreRepository franchiseStoreRepository;
    private final FranchiseStorePaymentRecordRepository franchiseStorePaymentRecordRepository;

    // 1. TẠO ORDER MỚI
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String orderId = orderIdGenerator.generateOrderId();

        Order order = Order.builder()
                .orderId(orderId)
                .statusOrder(OrderStatus.PENDING)
                .paymentOption(request.getPaymentOption())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .storeId(request.getStoreId())
                .orderDate(LocalDate.now())
                .note(request.getNote())
                .build();

        OrderDetail orderDetail = buildOrderDetail(order, request.getOrderDetail());
        order.assignOrderDetail(orderDetail);

        Order savedOrder = orderRepository.save(order);

        String orderDetailId = savedOrder.getOrderDetail().getOrderDetailId();
        BigDecimal totalAmount = orderDetail.getAmount();

        // ── Invoice ───────────────────────────────────────────────
        OrderInvoice invoice = OrderInvoice.builder()
                .orderInvoiceId("INV-" + savedOrder.getOrderId())
                .orderId(orderDetailId)
                .invoiceStatus("PENDING")
                .totalAmount(totalAmount)
                .build();
        orderInvoiceRepository.save(invoice);


        // ── Nếu PAY_AT_THE_END_OF_MONTH → tạo debt record ────────
        if (PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(request.getPaymentOption())) {
            FranchiseStorePaymentRecord debtRecord = new FranchiseStorePaymentRecord();
            debtRecord.setPaymentRecordId(UUID.randomUUID().toString());
            debtRecord.setStoreId(request.getStoreId());
            debtRecord.setDebtAmount(totalAmount);
            debtRecord.setStatus("PENDING");
            debtRecord.setCreatedAt(LocalDateTime.now());
            franchiseStorePaymentRecordRepository.save(debtRecord);

            // Cập nhật deptStatus = true cho store
            franchiseStoreRepository.findById(request.getStoreId()).ifPresent(store -> {
                store.setDeptStatus(true);
                franchiseStoreRepository.save(store);
            });

            log.info("Order {} → debt record created for store {}, amount={}",
                    orderId, request.getStoreId(), totalAmount);
        }

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

    // 5. UPDATE ORDER STATUS
//    @Transactional
//    public OrderResponse updateOrderStatus(String orderId, OrderUpdateRequest updateRequest) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
//
//        OrderStatus currentStatus = order.getStatusOrder();
//        OrderStatus newStatus = updateRequest.getNewStatus();
//        statusValidator.validateTransition(currentStatus, newStatus);
//
//        order.setStatusOrder(newStatus);
//
//        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
//            log.info("Order {} status change note: {}", orderId, updateRequest.getNote());
//        }
//
//        Order savedOrder = orderRepository.save(order);
//        log.info("Order {} status updated: {} → {}", orderId, currentStatus, newStatus);
//        return toResponse(savedOrder);
//    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderUpdateRequest updateRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatusOrder();
        OrderStatus newStatus = updateRequest.getNewStatus();

        // ✅ Bỏ statusValidator.validateTransition(currentStatus, newStatus);

        order.setStatusOrder(newStatus);

        if (updateRequest.getNote() != null && !updateRequest.getNote().isEmpty()) {
            order.setNote(updateRequest.getNote());
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng đã bị hủy rồi");
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Không thể hủy đơn đã thanh toán. Vui lòng hoàn tiền trước.");
        }

        // ── Cập nhật status order ─────────────────────────────────
        order.setStatusOrder(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        if (reason != null && !reason.isEmpty()) {
            order.setNote(reason);
        }

        // ── Nếu là PAY_AT_THE_END_OF_MONTH → xóa debt record ────
        if (PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(order.getPaymentOption())) {
            BigDecimal totalAmount = order.getOrderDetail().getAmount();

            List<FranchiseStorePaymentRecord> debtRecords =
                    franchiseStorePaymentRecordRepository.findByStoreId(order.getStoreId())
                            .stream()
                            .filter(r -> "PENDING".equals(r.getStatus())
                                    && r.getDebtAmount().compareTo(totalAmount) == 0)
                            .collect(Collectors.toList());

            franchiseStorePaymentRecordRepository.deleteAll(debtRecords);
            log.info("Order {} cancelled → deleted {} debt record(s)", orderId, debtRecords.size());

            // Reset deptStatus nếu không còn nợ
            long remainingDebt = franchiseStorePaymentRecordRepository
                    .findByStoreId(order.getStoreId())
                    .stream()
                    .filter(r -> "PENDING".equals(r.getStatus()))
                    .count();

            if (remainingDebt == 0) {
                franchiseStoreRepository.findById(order.getStoreId()).ifPresent(store -> {
                    store.setDeptStatus(false);
                    franchiseStoreRepository.save(store);
                    log.info("Store {} deptStatus reset to false", order.getStoreId());
                });
            }
        }

        // ── Cập nhật invoice → CANCELLED ─────────────────────────
        orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
            invoice.setInvoiceStatus("CANCELLED");
            orderInvoiceRepository.save(invoice);
            log.info("Invoice of order {} → CANCELLED", orderId);
        });

        Order saved = orderRepository.save(order);
        log.info("Order {} → CANCELLED, reason: {}", orderId, reason);
        return toResponse(saved);
    }

    // 9.LẤY ORDER DETAIL THEO ORDER ID
    @Transactional
    public OrderDetailResponse getOrderDetailByOrderId(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderDetail orderDetail = order.getOrderDetail();
        if (orderDetail == null) {
            throw new EntityNotFoundException("OrderDetail not found for order: " + orderId);
        }

        return toOrderDetailResponse(orderDetail);
    }

    // 10.✅ CASH: chuyển payment status → SUCCESS ngay lập tức
    @Transactional
    public OrderResponse payByCash(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw new IllegalStateException(
                    "Đơn hàng này không dùng phương thức CASH. Phương thức hiện tại: "
                            + order.getPaymentMethod());
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Đơn hàng này đã được thanh toán rồi");
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        Order saved = orderRepository.save(order);
        log.info("Order {} paid by CASH → payment status: SUCCESS", orderId);
        return toResponse(saved);
    }

    // 11.✅ Đổi PaymentMethod (chỉ cho phép đổi khi chưa thanh toán)
    @Transactional
    public OrderResponse changePaymentMethod(String orderId, PaymentMethod newMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Không thể đổi phương thức thanh toán vì đơn đã được thanh toán");
        }

        PaymentMethod oldMethod = order.getPaymentMethod();
        order.setPaymentMethod(newMethod);
        Order saved = orderRepository.save(order);
        log.info("Order {} payment method changed: {} → {}", orderId, oldMethod, newMethod);
        return toResponse(saved);
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
    public OrderResponse confirmOrder(String orderId, Integer priorityLevel) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order. Current status is: " + order.getStatusOrder());
        }

        // ✅ Validate priority (1, 2, 3)
        if (priorityLevel == null || priorityLevel < 1 || priorityLevel > 3) {
            throw new IllegalArgumentException("Priority level must be 1 (HIGH), 2 (MEDIUM), or 3 (LOW)");
        }

        order.setStatusOrder(OrderStatus.IN_PROGRESS);
        order.setPriorityLevel(priorityLevel);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} confirmed: PENDING → IN_PROGRESS, priority set to {}", orderId, priorityLevel);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse changePaymentOption(String orderId, PaymentOption newOption) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Không thể đổi payment option vì đơn đã được thanh toán");
        }

        PaymentOption oldOption = order.getPaymentOption();

        if (oldOption.equals(newOption)) {
            throw new IllegalStateException("Payment option mới phải khác option hiện tại: " + oldOption);
        }

        order.setPaymentOption(newOption);

        // ── Đổi sang PAY_AT_THE_END_OF_MONTH → tạo debt record ──────
        if (PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(newOption)) {
            BigDecimal totalAmount = order.getOrderDetail().getAmount();

            FranchiseStorePaymentRecord debtRecord = new FranchiseStorePaymentRecord();
            debtRecord.setPaymentRecordId(UUID.randomUUID().toString());
            debtRecord.setStoreId(order.getStoreId());
            debtRecord.setDebtAmount(totalAmount);
            debtRecord.setStatus("PENDING");
            debtRecord.setCreatedAt(LocalDateTime.now());
            franchiseStorePaymentRecordRepository.save(debtRecord);

            franchiseStoreRepository.findById(order.getStoreId()).ifPresent(store -> {
                store.setDeptStatus(true);
                franchiseStoreRepository.save(store);
            });

            log.info("Order {} → PAY_AT_THE_END_OF_MONTH, debt record created", orderId);
        }

        // ── Đổi về PAY_AFTER_ORDER → xóa debt record của order này ──
        if (PaymentOption.PAY_AFTER_ORDER.equals(newOption)
                && PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(oldOption)) {

            BigDecimal totalAmount = order.getOrderDetail().getAmount();

            // Xóa debt record tương ứng với order này (cùng storeId, cùng amount, status PENDING)
            List<FranchiseStorePaymentRecord> debtRecords =
                    franchiseStorePaymentRecordRepository.findByStoreId(order.getStoreId())
                            .stream()
                            .filter(r -> "PENDING".equals(r.getStatus())
                                    && r.getDebtAmount().compareTo(totalAmount) == 0)
                            .collect(Collectors.toList());

            franchiseStorePaymentRecordRepository.deleteAll(debtRecords);

            // Nếu không còn debt record PENDING nào → reset deptStatus = false
            long remainingDebt = franchiseStorePaymentRecordRepository
                    .findByStoreId(order.getStoreId())
                    .stream()
                    .filter(r -> "PENDING".equals(r.getStatus()))
                    .count();

            if (remainingDebt == 0) {
                franchiseStoreRepository.findById(order.getStoreId()).ifPresent(store -> {
                    store.setDeptStatus(false);
                    franchiseStoreRepository.save(store);
                });
            }

            log.info("Order {} → PAY_AFTER_ORDER, debt record removed, remaining debt count={}",
                    orderId, remainingDebt);
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} payment option changed: {} → {}", orderId, oldOption, newOption);
        return toResponse(saved);
    }

    // ==================== HELPER METHODS ====================



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
                .paymentMethod(order.getPaymentMethod())   // ← thêm
                .paymentStatus(order.getPaymentStatus())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .note(order.getNote())
                .build();
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail) {
        List<OrderDetailItemResponse> items = orderDetail.getOrderDetailItems().stream()
                .map(item -> (OrderDetailItemResponse) OrderDetailItemResponse.builder()
                        .centralFoodId(item.getOrderDetailItemId())
                        .centralFoodId(item.getCentralFoodId())
                        .foodName(item.getFoodName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalAmount(item.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderDetailId(orderDetail.getOrderDetailId())
                .orderId(orderDetail.getOrderId())
                .amount(orderDetail.getAmount())
                .items(items)
                .build();
    }


}