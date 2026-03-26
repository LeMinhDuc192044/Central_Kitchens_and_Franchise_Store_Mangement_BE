package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailItemResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderDetailResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.OrderResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Integer TIME_TO_PAY = 1;

    private final OrderRepository orderRepository;
    private final OrderStatusValidator statusValidator;
    private final OrderIdGenerator orderIdGenerator;
    private final PriorityLevelValidator priorityValidator;
    private final OrderInvoiceRepository orderInvoiceRepository;
    private final CentralFoodsRepository centralFoodsRepository;
    private final FranchiseStoreRepository franchiseStoreRepository;
    private final FranchiseStorePaymentRecordRepository franchiseStorePaymentRecordRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String orderId = orderIdGenerator.generateOrderId();

        // ── Validate PaymentMethod theo PaymentOption ─────────────
        if ((PaymentOption.PAY_AFTER_ORDER.equals(request.getPaymentOption())
                || PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(request.getPaymentOption()))
                && request.getPaymentMethod() != PaymentMethod.CREDIT) {
            throw new IllegalArgumentException(
                    "Payment option " + request.getPaymentOption()
                            + " chỉ hỗ trợ phương thức CREDIT, không hỗ trợ: "
                            + request.getPaymentMethod());
        }

        LocalDateTime now = LocalDateTime.now();

        Order order = Order.builder()
                .orderId(orderId)
                .statusOrder(OrderStatus.PENDING)
                .paymentOption(request.getPaymentOption())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .storeId(request.getStoreId())
                .orderDate(LocalDate.now())
                .note(request.getNote())
                .orderDate(request.getOrderDate())
                .createdAt(now)
                .build();

        OrderDetail orderDetail = buildOrderDetail(order, request.getOrderDetail());
        order.assignOrderDetail(orderDetail);

        Order savedOrder = orderRepository.save(order);

        String orderDetailId = savedOrder.getOrderDetail().getOrderDetailId();
        BigDecimal totalAmount = orderDetail.getAmount();

        // ── Nếu PAY_AT_THE_END_OF_MONTH → tạo debt record TRƯỚC ──
        String paymentRecordId = null;

        if (PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(request.getPaymentOption())) {

            // ── Find or create ONE MONTHLY record per store ────────────────────────
            FranchiseStorePaymentRecord monthlyRecord = franchiseStorePaymentRecordRepository
                    .findByStoreIdAndStatusAndRecordType(
                            request.getStoreId(), PaymentStatus.PENDING, PaymentRecordType.MONTHLY)
                    .orElseGet(() -> {
                        log.info("Creating new MONTHLY record for store [{}]", request.getStoreId());
                        return FranchiseStorePaymentRecord.builder()
                                .paymentRecordId(UUID.randomUUID().toString())
                                .storeId(request.getStoreId())
                                .debtAmount(BigDecimal.ZERO)
                                .status(PaymentStatus.PENDING)
                                .recordType(PaymentRecordType.MONTHLY)   // ← MONTHLY type
                                .createdAt(LocalDateTime.now())
                                .payDate(LocalDateTime.now().plusMonths(TIME_TO_PAY))
                                .build();
                    });

            // ── Accumulate amount + link order ─────────────────────────────────────
            monthlyRecord.setDebtAmount(monthlyRecord.getDebtAmount().add(totalAmount));
            monthlyRecord.getOrders().add(savedOrder);
            FranchiseStorePaymentRecord saved = franchiseStorePaymentRecordRepository.save(monthlyRecord);
            paymentRecordId = saved.getPaymentRecordId();

            log.info("Order [{}] added to MONTHLY record [{}], total={}",
                    orderId, paymentRecordId, monthlyRecord.getDebtAmount());
        }

        // ── Invoice ───────────────────────────────────────────────
        OrderInvoice invoice = OrderInvoice.builder()
                .orderInvoiceId("INV-" + savedOrder.getOrderId())
                .orderId(savedOrder.getOrderId())
                .paymentType(String.valueOf(request.getPaymentMethod()))
                .invoiceStatus("PENDING")
                .totalAmount(totalAmount)
                .paymentRecordId(paymentRecordId) // 👈 set String FK trực tiếp (null nếu không phải PAY_AT_THE_END_OF_MONTH)
                .build();
        orderInvoiceRepository.save(invoice);

        log.info("Created order {} with {} item(s)",
                orderId, request.getOrderDetail().getItems().size());

        return toResponse(savedOrder);
    }

    @Transactional
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrder(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 2. LẤY ORDER THEO ID
    @Transactional
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    // 3. LẤY TẤT CẢ ORDERS
    @Transactional
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }



    // 5. UPDATE ORDER STATUS — không cần OrderUpdateRequest nữa
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatusOrder();

        // Validate transition hợp lệ
        //statusValidator.validateTransition(currentStatus, newStatus, false);

        order.setStatusOrder(newStatus);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated: {} → {}", orderId, currentStatus, newStatus);
        return toResponse(savedOrder);
    }

    // Lấy danh sách status hợp lệ tiếp theo của một order
    @Transactional
    public List<OrderStatus> getAllowedNextStatuses(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        return new ArrayList<>(statusValidator.getAllowedTransitions(order.getStatusOrder()));
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
    @Transactional
    public List<OrderResponse> getOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreId(storeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId, String cancelReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng đã bị hủy rồi");
        }

        // ✅ Chỉ PAY_AFTER_ORDER + đã thanh toán → mới đánh dấu PENDING_REFUND
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            if (PaymentOption.PAY_AFTER_ORDER.equals(order.getPaymentOption())) {
                order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                log.info("Order {} (PAY_AFTER_ORDER) đã thanh toán → hủy và chờ hoàn tiền", orderId);
            } else {
                // PAY_AT_THE_END_OF_MONTH đã SUCCESS thì không cho hủy
                throw new IllegalStateException(
                        "Không thể hủy đơn đã thanh toán với option: " + order.getPaymentOption());
            }
        }

        order.setStatusOrder(OrderStatus.CANCELLED);
        if (cancelReason != null && !cancelReason.isEmpty()) {
            order.setCancelReason(cancelReason);
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
        log.info("Order {} → CANCELLED, cancelReason: {}", orderId, cancelReason);
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
    @Transactional
    public List<OrderResponse> getAllOrdersWithPendingStatusByStoreId(String storeId) {
        return orderRepository.findByStoreIdAndStatusOrder(storeId, OrderStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 13. LẤY TẤT CẢ ORDERS PENDING
    @Transactional
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
            debtRecord.setStatus(PaymentStatus.PENDING);
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

    @Transactional
    public OrderResponse editMultipleOrderDetailItems(String orderId, EditOrderItemsRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() == OrderStatus.CANCELLED
                || order.getStatusOrder() == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Không thể chỉnh sửa item vì order đang ở trạng thái: " + order.getStatusOrder());
        }

        OrderDetail orderDetail = order.getOrderDetail();

        for (EditOrderItemRequest itemRequest : request.getItems()) {
            OrderDetailItem targetItem = orderDetail.getOrderDetailItems().stream()
                    .filter(item -> item.getCentralFoodId().equals(itemRequest.getCentralFoodId()))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Item không tồn tại: " + itemRequest.getCentralFoodId()));

            CentralFoods food = centralFoodsRepository.findById(itemRequest.getCentralFoodId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + itemRequest.getCentralFoodId()));

            BigDecimal unitPrice = BigDecimal.valueOf(food.getUnitPriceFood());
            targetItem.setQuantity(itemRequest.getQuantity());
            targetItem.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        // Recalculate tổng amount
        BigDecimal newAmount = orderDetail.getOrderDetailItems().stream()
                .map(OrderDetailItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orderDetail.setAmount(newAmount);

        return toResponse(orderRepository.save(order));
    }

    // GET ALL CANCELLED ORDERS
    @Transactional
    public List<OrderResponse> getAllCancelledOrders() {
        return orderRepository.findByStatusOrder(OrderStatus.CANCELLED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET CANCELLED ORDERS BY STORE ID
    @Transactional
    public List<OrderResponse> getCancelledOrdersByStoreId(String storeId) {
        return orderRepository.findByStoreIdAndStatusOrder(storeId, OrderStatus.CANCELLED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET ORDERS BY CREATED AT DATE
    @Transactional
    public List<OrderResponse> getOrdersByCreatedAt(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay   = date.plusDays(1).atStartOfDay();

        return orderRepository.findByCreatedAtBetween(startOfDay, endOfDay).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET NEW ORDERS TODAY
    @Transactional
    public List<OrderResponse> getNewOrdersToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay   = today.plusDays(1).atStartOfDay();

        return orderRepository.findByCreatedAtBetween(startOfDay, endOfDay).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================



    private OrderDetail buildOrderDetail(Order order, OrderDetailRequest request) {
        String orderDetailId = IdGeneratorUtil.generateOrderDetailId();

        OrderDetail orderDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .orderId(order.getOrderId())
                .amount(BigDecimal.ZERO)
                .build();

        for (OrderDetailItemRequest itemRequest : request.getItems()) {
            OrderDetailItem item = buildOrderDetailItem(orderDetail, itemRequest);
            orderDetail.addOrderDetailItem(item);
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
                .foodName(centralFood.getFoodName())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .build();
    }

    private OrderResponse toResponse(Order order) {
        OrderDetailResponse detailResponse = null;
        if (order.getOrderDetail() != null) {
            detailResponse = toOrderDetailResponse(order.getOrderDetail());
        }

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .priorityLevel(order.getPriorityLevel())
                .paymentOption(order.getPaymentOption())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderDate(order.getOrderDate())
                .statusOrder(order.getStatusOrder())
                .storeId(order.getStoreId())
                .note(order.getNote())
                .orderDetail(detailResponse)
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())// ✅ Thêm
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