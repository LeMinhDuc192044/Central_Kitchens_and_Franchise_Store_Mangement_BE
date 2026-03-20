package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.VNPayProperties;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CreatePaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResultResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.VNPayResponseCode;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.VNPayUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayProperties vnPayProperties;
    private final PaymentRepository paymentRepository;
    private final OrderInvoiceRepository orderInvoiceRepository;
    private final OrderRepository orderRepository;
    private final FranchiseStoreRepository franchiseStoreRepository;
    private final FranchiseStorePaymentRecordRepository franchisePaymentRecordRepository;
    private final FranchiseStorePaymentMethodRepository franchisePaymentMethodRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    // ============================================================
    // 1. TẠO URL THANH TOÁN
    // ============================================================

    @Transactional
    public CreatePaymentResponse createPaymentUrlByOrder(String orderId, HttpServletRequest httpRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.CREDIT) {
            throw new RuntimeException("Chỉ CREDIT mới được thanh toán qua VNPay.");
        }

        if (!PaymentOption.PAY_AFTER_ORDER.equals(order.getPaymentOption())) {
            throw new RuntimeException("Đơn hàng " + orderId + " không hỗ trợ thanh toán ngay.");
        }

        // ===== NEW: check giao dịch gần nhất theo orderId =====
        List<PaymentRecord> records = paymentRecordRepository.findAllByOrderId(orderId);
        PaymentRecord latest = records.stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);

        if (latest != null) {
            String st = latest.getStatus();

            if ("SUCCESS".equals(st) || "PAID".equals(st)) {
                throw new RuntimeException("Đơn hàng " + orderId + " đã thanh toán thành công.");
            }

            if ("PENDING".equals(st)) {
                // Nếu muốn cho tạo lại ngay: đóng giao dịch cũ
                latest.setStatus("EXPIRED");
                latest.setResponseCode("24");
                latest.setResponseMessage("Hết thời gian thanh toán, tạo lại giao dịch mới");
                paymentRecordRepository.save(latest);

                paymentRepository.findByTxnRef(latest.getTxnRef()).ifPresent(p -> {
                    p.setStatus(PaymentStatus.FAILED); // hoặc EXPIRED nếu enum có
                    paymentRepository.save(p);
                });

                orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
                    invoice.setHasPendingTransaction(false);
                    orderInvoiceRepository.save(invoice);
                });
            }
        }

        OrderDetail orderDetail = order.getOrderDetail();
        if (orderDetail == null || orderDetail.getAmount() == null) {
            throw new RuntimeException("Đơn hàng không có thông tin chi tiết");
        }

        Long totalAmount = orderDetail.getAmount().longValue();
        if (totalAmount <= 0) throw new RuntimeException("Đơn hàng không có giá trị");

        String txnRef = VNPayUtil.generateTxnRef();
        String ipAddress = VNPayUtil.getIpAddress(httpRequest);
        long amountInVNPay = totalAmount * 100;

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayProperties.getVersion());
        vnpParams.put("vnp_Command", vnPayProperties.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVNPay));
        vnpParams.put("vnp_CurrCode", vnPayProperties.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", VNPayUtil.getCurrentDateTime());
        vnpParams.put("vnp_ExpireDate", VNPayUtil.getExpireDateTime(15));
        vnpParams.put("vnp_BankCode", "NCB");

        String hashData = VNPayUtil.buildHashData(vnpParams);
        String secureHash = VNPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", secureHash);

        String paymentUrl = vnPayProperties.getUrl() + "?" + VNPayUtil.buildQueryString(vnpParams);

        Payment payment = new Payment();
        payment.setTxnRef(txnRef);
        payment.setAmount(totalAmount);
        payment.setIpAddress(ipAddress);
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        PaymentRecord newRecord = PaymentRecord.builder()
                .orderId(orderId)
                .txnRef(txnRef)
                .amount(totalAmount)
                .status("PENDING")
                .build();
        paymentRecordRepository.save(newRecord);

        orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
            invoice.setHasPendingTransaction(true);
            invoice.setTxnRef(txnRef);
            orderInvoiceRepository.save(invoice);
        });

        return CreatePaymentResponse.builder()
                .txnRef(txnRef)
                .paymentUrl(paymentUrl)
                .amount(totalAmount)
                .orderIds(List.of(orderId))
                .build();
    }


    @Transactional
    public CreatePaymentResponse createDebtPaymentByStore(
            String storeId, HttpServletRequest httpRequest) {

        List<FranchiseStorePaymentRecord> records = franchisePaymentRecordRepository.findByStoreId(storeId);

        FranchiseStore store = franchiseStoreRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store không tồn tại: " + storeId));

        List<String> storeMethods = franchisePaymentMethodRepository.findByStoreId(storeId)
                .stream()
                .map(FranchiseStorePaymentMethod::getPaymentMethod)
                .collect(Collectors.toList());

        if (!storeMethods.contains("CREDIT")) {
            throw new IllegalStateException(
                    "Store " + storeId + " không hỗ trợ CREDIT. Không thể thanh toán qua VNPay.");
        }

        if (!store.isDeptStatus()) {
            throw new IllegalStateException("Store " + storeId + " không có nợ cần thanh toán");
        }

        if (records.isEmpty()) {
            throw new RuntimeException("Không có bản ghi nợ nào cho store: " + storeId);
        }

        BigDecimal totalDebt = records.stream()
                .map(FranchiseStorePaymentRecord::getDebtAmount)
                .filter(amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Store " + storeId + " không có nợ cần thanh toán");
        }

        Long totalAmount = totalDebt.longValue();
        String txnRef = VNPayUtil.generateTxnRef();
        String ipAddress = VNPayUtil.getIpAddress(httpRequest);
        long amountInVNPay = totalAmount * 100;

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayProperties.getVersion());
        vnpParams.put("vnp_Command", vnPayProperties.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVNPay));
        vnpParams.put("vnp_CurrCode", vnPayProperties.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan no store " + storeId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", VNPayUtil.getCurrentDateTime());
        vnpParams.put("vnp_ExpireDate", VNPayUtil.getExpireDateTime(15));
        vnpParams.put("vnp_BankCode", "NCB");

        String hashData = VNPayUtil.buildHashData(vnpParams);
        String secureHash = VNPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", secureHash);

        String queryString = VNPayUtil.buildQueryString(vnpParams);
        String paymentUrl = vnPayProperties.getUrl() + "?" + queryString;

        Payment payment = new Payment();
        payment.setTxnRef(txnRef);
        payment.setAmount(totalAmount);
        payment.setIpAddress(ipAddress);
        payment.setStoreId(storeId);
        paymentRepository.save(payment);


        log.info("Tạo debt payment: storeId={}, txnRef={}, totalDebt={}", storeId, txnRef, totalAmount);

        return CreatePaymentResponse.builder()
                .txnRef(txnRef)
                .paymentUrl(paymentUrl)
                .amount(totalAmount)
                .orderIds(List.of(storeId))
                .build();
    }


    @Transactional
    public PaymentResultResponse processVNPayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> vnpParams.put(key, values[0]));

        String vnpSecureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String hashData = VNPayUtil.buildHashData(vnpParams);
        String checkHash = VNPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        if (!checkHash.equalsIgnoreCase(vnpSecureHash)) {
            throw new RuntimeException("Chữ ký không hợp lệ");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String bankCode = vnpParams.get("vnp_BankCode");
        String bankTranNo = vnpParams.get("vnp_BankTranNo");
        String cardType = vnpParams.get("vnp_CardType");
        String vnpayTxnNo = vnpParams.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + txnRef));

        payment.setResponseCode(responseCode);
        payment.setBankCode(bankCode);
        payment.setBankTranNo(bankTranNo);
        payment.setCardType(cardType);
        payment.setVnpayTxnNo(vnpayTxnNo);

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            String orderId = payment.getOrderId();
            String storeId = payment.getStoreId();

            // ── Xử lý debt payment ────────────────────────────────
            if (storeId != null && orderId == null) {
                List<FranchiseStorePaymentRecord> debtRecords =
                        franchisePaymentRecordRepository.findByStoreId(storeId);
                franchisePaymentRecordRepository.deleteAll(debtRecords);

                franchiseStoreRepository.findById(storeId).ifPresent(store -> {
                    store.setDeptStatus(false);
                    franchiseStoreRepository.save(store);
                    log.info("Store {} debt cleared after payment txnRef={}", storeId, txnRef);
                });

                // ✅ Update paymentStatus + invoice các order PAY_AT_THE_END_OF_MONTH → SUCCESS
                List<Order> storeOrders = orderRepository.findByStoreId(storeId);
                storeOrders.stream()
                        .filter(o -> PaymentStatus.PENDING.equals(o.getPaymentStatus())
                                && PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(o.getPaymentOption()))
                        .forEach(o -> {
                            o.setPaymentStatus(PaymentStatus.SUCCESS);
                            orderRepository.save(o);

                            // ✅ Update invoice của từng order
                            orderInvoiceRepository.findByOrderId(o.getOrderId()).ifPresent(invoice -> {
                                invoice.setInvoiceStatus("PAID");
                                invoice.setPaymentType("VNPAY");
                                invoice.setTotalAmount(BigDecimal.valueOf(o.getOrderDetail().getAmount().longValue()));
                                invoice.setPaidDate(LocalDate.now());
                                invoice.setHasPendingTransaction(false);
                                orderInvoiceRepository.save(invoice);
                                log.info("Invoice of order {} → PAID after debt payment txnRef={}", o.getOrderId(), txnRef);
                            });

                            log.info("Order {} paymentStatus → SUCCESS after debt payment txnRef={}", o.getOrderId(), txnRef);
                        });
            }

            // ── Cập nhật invoice (PAY_AFTER_ORDER) ───────────────
            if (orderId != null) {
                orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
                    invoice.setInvoiceStatus("PAID");
                    //invoice.setPaymentType("VNPAY");
                    invoice.setTotalAmount(BigDecimal.valueOf(payment.getAmount()));
                    invoice.setPaidDate(LocalDate.now());
                    invoice.setHasPendingTransaction(false);
                    orderInvoiceRepository.save(invoice);
                    log.info("Invoice updated to PAID for orderId: {}", orderId);
                });

                // Cập nhật paymentStatus trên Order
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    orderRepository.save(order);
                    log.info("Order paymentStatus updated to SUCCESS for orderId: {}", orderId);
                });
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            // Reset hasPendingTransaction = false nếu thanh toán thất bại
            String orderId = payment.getOrderId();
            if (orderId != null) {
                orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
                    invoice.setHasPendingTransaction(false);
                    orderInvoiceRepository.save(invoice);
                    log.info("Invoice of order {} → hasPendingTransaction=false (payment FAILED)", orderId);
                });
            }
        }

        paymentRepository.save(payment);

        List<PaymentRecord> records = paymentRecordRepository.findAllByTxnRef(txnRef);
        records.forEach(record -> {
            record.setStatus(payment.getStatus().name());
            record.setResponseCode(responseCode);
            record.setResponseMessage(VNPayResponseCode.getMessage(responseCode));
            paymentRecordRepository.save(record);
        });

        log.info("VNPay return: txnRef={}, responseCode={}", txnRef, responseCode);

        String message = VNPayResponseCode.getMessage(responseCode);
        return buildResult(payment, responseCode, message);
    }

    @Transactional
    public PaymentResultResponse refundPayment(String orderId, HttpServletRequest httpRequest) {
        log.info("=== REFUND START (DEV MODE): orderId={}", orderId);

        // ✅ Bắt buộc hủy đơn trước mới được hoàn tiền
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatusOrder() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Chỉ có thể hoàn tiền khi đơn hàng đã bị hủy. Trạng thái hiện tại: "
                            + order.getStatusOrder());
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .stream()
                .filter(p -> PaymentStatus.SUCCESS.equals(p.getStatus())
                        || PaymentStatus.PENDING_REFUND.equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy giao dịch hợp lệ để hoàn tiền cho order: " + orderId));

        // ✅ DEV MODE: Bỏ qua VNPay API, force REFUNDED trực tiếp
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // ✅ Cập nhật Order → REFUNDED
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);
        log.info("Order {} paymentStatus → REFUNDED", orderId);

        // ✅ Cập nhật Invoice → REFUNDED
        orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
            invoice.setInvoiceStatus("REFUNDED");
            invoice.setHasPendingTransaction(false);
            orderInvoiceRepository.save(invoice);
            log.info("Invoice of order {} → REFUNDED", orderId);
        });

        // ✅ Cập nhật PaymentRecord → REFUNDED
        List<PaymentRecord> records = paymentRecordRepository.findAllByTxnRef(payment.getTxnRef());
        records.forEach(record -> {
            record.setStatus("REFUNDED");
            record.setResponseCode("00");
            record.setResponseMessage("Hoàn tiền thành công (dev mode)");
            paymentRecordRepository.save(record);
        });

        log.info("Refund SUCCESS (dev mode): orderId={}", orderId);
        return buildResult(payment, "00", "Hoàn tiền thành công (dev mode)");
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private PaymentResultResponse buildResult(Payment payment, String responseCode, String message) {
        return PaymentResultResponse.builder()
                .txnRef(payment.getTxnRef())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .responseCode(responseCode)
                .responseMessage(message)
                .bankCode(payment.getBankCode())
                .bankTranNo(payment.getBankTranNo())
                .cardType(payment.getCardType())
                .vnpayTxnNo(payment.getVnpayTxnNo())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public PaymentResultResponse getPaymentByTxnRef(String txnRef) {
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + txnRef));
        String message = VNPayResponseCode.getMessage(payment.getResponseCode());
        return buildResult(payment, payment.getResponseCode(), message);
    }
}