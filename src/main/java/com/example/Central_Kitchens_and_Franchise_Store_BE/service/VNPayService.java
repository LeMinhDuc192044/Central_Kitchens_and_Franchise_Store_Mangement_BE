package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.VNPayProperties;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CreatePaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResultResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.VNPayResponseCode;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.VNPayUtil;
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

    // ============================================================
    // 1. TẠO URL THANH TOÁN
    // ============================================================
    private final PaymentRecordRepository paymentRecordRepository;

    @Transactional
    public CreatePaymentResponse createPaymentUrlByOrder(String orderId, HttpServletRequest httpRequest) {
        // ✅ Check xem orderId đã từng giao dịch chưa
        if (paymentRecordRepository.existsByOrderId(orderId)) {
            throw new RuntimeException("Đơn hàng " + orderId + " đã được tạo giao dịch thanh toán trước đó");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        // ✅ Chỉ CREDIT mới được tạo VNPay URL
        if (order.getPaymentMethod() != PaymentMethod.CREDIT) {
            throw new RuntimeException(
                    "Đơn hàng " + orderId + " dùng phương thức " + order.getPaymentMethod()
                            + ". Chỉ CREDIT mới được thanh toán qua VNPay.");
        }

        if (!PaymentOption.PAY_AFTER_ORDER.equals(order.getPaymentOption())) {
            throw new RuntimeException(
                    "Đơn hàng " + orderId + " không hỗ trợ thanh toán ngay. " +
                            "Hình thức thanh toán hiện tại: " + order.getPaymentOption()
            );
        }

        OrderDetail orderDetail = order.getOrderDetail();
        if (orderDetail == null || orderDetail.getAmount() == null) {
            throw new RuntimeException("Đơn hàng không có thông tin chi tiết");
        }

        Long totalAmount = orderDetail.getAmount().longValue();

        if (totalAmount <= 0) {
            throw new RuntimeException("Đơn hàng không có giá trị");
        }

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

        String queryString = VNPayUtil.buildQueryString(vnpParams);
        String paymentUrl = vnPayProperties.getUrl() + "?" + queryString;

        // Lưu Payment
        Payment payment = new Payment();
        payment.setTxnRef(txnRef);
        payment.setAmount(totalAmount);
        payment.setIpAddress(ipAddress);
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        // ✅ Lưu PaymentRecord
        PaymentRecord record = PaymentRecord.builder()
                .orderId(orderId)
                .txnRef(txnRef)
                .amount(totalAmount)
                .status("PENDING")
                .build();
        paymentRecordRepository.save(record);

        log.info("Tạo payment URL: orderId={}, txnRef={}, amount={}", orderId, txnRef, totalAmount);

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

        // ✅ Lấy tất cả payment record của store
        List<FranchiseStorePaymentRecord> records = franchisePaymentRecordRepository.findByStoreId(storeId);

        // ✅ Check store tồn tại
        FranchiseStore store = franchiseStoreRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store không tồn tại: " + storeId));

        // ✅ Check paymentMethod của store phải là CREDIT
        List<String> storeMethods = franchisePaymentMethodRepository.findByStoreId(storeId)
                .stream()
                .map(FranchiseStorePaymentMethod::getPaymentMethod)
                .collect(Collectors.toList());

        if (!storeMethods.contains("CREDIT")) {
            throw new IllegalStateException(
                    "Store " + storeId + " không hỗ trợ CREDIT. Không thể thanh toán qua VNPay.");
        }

        // ✅ Check store đang có nợ
        if (!store.isDeptStatus()) {
            throw new IllegalStateException("Store " + storeId + " không có nợ cần thanh toán");
        }

        if (records.isEmpty()) {
            throw new RuntimeException("Không có bản ghi nợ nào cho store: " + storeId);
        }

        // ✅ Tính tổng tiền nợ
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

        // ✅ Lưu Payment
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
        // Lấy toàn bộ params VNPay gửi về
        Map<String, String> vnpParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> vnpParams.put(key, values[0]));

        // Lấy secure hash VNPay gửi về để verify
        String vnpSecureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        // Verify chữ ký
        String hashData = VNPayUtil.buildHashData(vnpParams);
        String checkHash = VNPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        if (!checkHash.equalsIgnoreCase(vnpSecureHash)) {
            throw new RuntimeException("Chữ ký không hợp lệ");
        }

        // Lấy thông tin từ params
        String txnRef = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String bankCode = vnpParams.get("vnp_BankCode");
        String bankTranNo = vnpParams.get("vnp_BankTranNo");
        String cardType = vnpParams.get("vnp_CardType");
        String vnpayTxnNo = vnpParams.get("vnp_TransactionNo");

        // Tìm payment trong DB
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + txnRef));

        // Cập nhật thông tin payment
        payment.setResponseCode(responseCode);
        payment.setBankCode(bankCode);
        payment.setBankTranNo(bankTranNo);
        payment.setCardType(cardType);
        payment.setVnpayTxnNo(vnpayTxnNo);

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            String orderId = payment.getOrderId();
            String storeId = payment.getStoreId(); // ← lấy storeId

            // ── Xử lý debt payment (storeId != null, orderId == null) ────
            if (storeId != null && orderId == null) {
                // Xóa toàn bộ payment record nợ của store
                List<FranchiseStorePaymentRecord> debtRecords =
                        franchisePaymentRecordRepository.findByStoreId(storeId);
                franchisePaymentRecordRepository.deleteAll(debtRecords);

                // Reset deptStatus về false
                franchiseStoreRepository.findById(storeId).ifPresent(store -> {
                    store.setDeptStatus(false);
                    franchiseStoreRepository.save(store);
                    log.info("Store {} debt cleared after payment txnRef={}", storeId, txnRef);
                });
            }

            // ── Cập nhật invoice ──────────────────────────────────
            orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
                invoice.setInvoiceStatus("PAID");
                invoice.setPaymentType("VNPAY");
                invoice.setTotalAmount(BigDecimal.valueOf(payment.getAmount()));
                invoice.setPaidDate(LocalDate.now());
                orderInvoiceRepository.save(invoice);
                log.info("Invoice updated to PAID for orderId: {}", orderId);
            });
            // ─────────────────────────────────────────────────────
            // ✅ THÊM: Cập nhật paymentStatus trên Order
            if (orderId != null) {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    orderRepository.save(order);
                    log.info("Order paymentStatus updated to SUCCESS for orderId: {}", orderId);
                });
            }
            // ─────────────────────────────────────────────────────
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);

        // Cập nhật PaymentRecord
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
    public PaymentResultResponse refundPayment(String orderId, String refundType, HttpServletRequest httpRequest) {
        log.info("=== REFUND START: orderId={}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .stream()
                .filter(p -> PaymentStatus.SUCCESS.equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch SUCCESS cho order: " + orderId));

        if (payment.getVnpayTxnNo() == null) {
            throw new RuntimeException("Giao dịch chưa có mã VNPay TransactionNo");
        }

        String ipAddress = VNPayUtil.getIpAddress(httpRequest);
        String requestId = VNPayUtil.generateTxnRef();
        String createDate = VNPayUtil.getCurrentDateTime();

        // ✅ Build JSON body theo đúng format VNPay
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_RequestId", requestId);
        vnpParams.put("vnp_Version", vnPayProperties.getVersion());
        vnpParams.put("vnp_Command", "refund");
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_TransactionType", refundType);
        vnpParams.put("vnp_TxnRef", payment.getTxnRef());
        vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount() * 100));
        vnpParams.put("vnp_OrderInfo", "Hoan tien don hang " + orderId);
        vnpParams.put("vnp_TransactionNo", payment.getVnpayTxnNo());
        vnpParams.put("vnp_TransactionDate", createDate);
        vnpParams.put("vnp_CreateBy", "system");
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_IpAddr", ipAddress);

        // ✅ Hash đúng thứ tự theo tài liệu VNPay refund
        String hashData = vnpParams.get("vnp_RequestId") + "|" +
                vnpParams.get("vnp_Version") + "|" +
                vnpParams.get("vnp_Command") + "|" +
                vnpParams.get("vnp_TmnCode") + "|" +
                vnpParams.get("vnp_TransactionType") + "|" +
                vnpParams.get("vnp_TxnRef") + "|" +
                vnpParams.get("vnp_Amount") + "|" +
                vnpParams.get("vnp_TransactionNo") + "|" +
                vnpParams.get("vnp_TransactionDate") + "|" +
                vnpParams.get("vnp_CreateBy") + "|" +
                vnpParams.get("vnp_CreateDate") + "|" +
                vnpParams.get("vnp_IpAddr") + "|" +
                vnpParams.get("vnp_OrderInfo");

        String secureHash = VNPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", secureHash);

        // ✅ Gửi JSON thay vì form-urlencoded
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonBody = mapper.writeValueAsString(vnpParams);

            java.net.URL url = new java.net.URL(vnPayProperties.getRefundUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");  // ← JSON
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // ✅ Đọc response kể cả khi lỗi
            java.io.InputStream is = conn.getResponseCode() >= 400
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            log.info("VNPay refund raw response: {}", response);

            Map<String, String> refundResponse = mapper.readValue(response.toString(), Map.class);
            String responseCode = refundResponse.get("vnp_ResponseCode");

            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                String orderIdFromPayment = payment.getOrderId();
                log.info("Refund SUCCESS - updating order and invoice for orderId={}", orderIdFromPayment);

                if (orderIdFromPayment != null) {
                    // ✅ Update paymentStatus trên Order
                    orderRepository.findById(orderIdFromPayment).ifPresent(order -> {
                        order.setPaymentStatus(PaymentStatus.REFUNDED);
                        orderRepository.save(order);
                        log.info("Order {} paymentStatus → REFUNDED", orderIdFromPayment);
                    });

                    // ✅ Update invoice → REFUNDED
                    orderInvoiceRepository.findByOrderId(orderIdFromPayment).ifPresent(invoice -> {
                        invoice.setInvoiceStatus("REFUNDED");
                        orderInvoiceRepository.save(invoice);
                        log.info("Invoice of order {} → REFUNDED", orderIdFromPayment);
                    });
                }
            }

            log.info("Refund result: orderId={}, responseCode={}", orderId, responseCode);
            String message = VNPayResponseCode.getMessage(responseCode);
            return buildResult(payment, responseCode, message);

        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new RuntimeException("Hoàn tiền thất bại: " + e.getMessage());
        }
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
