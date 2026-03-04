package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.VNPayProperties;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CreatePaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentResultResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Order;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.OrderDetail;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Payment;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.PaymentRecord;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentOption;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderInvoiceRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.PaymentRecordRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.PaymentRepository;
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


    public List<PaymentResponse> getAllPayments(String status) {
        return paymentRepository.findAll().stream()
                .filter(p -> status == null || status.isBlank() ||
                        (p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status)))
                .map(p -> PaymentResponse.builder()
                        .id(p.getPaymentId())
                        .txnRef(p.getTxnRef())
                        .amount(p.getAmount())
                        .status(p.getStatus() != null ? p.getStatus().name() : null)
                        .responseCode(p.getResponseCode())
                        .ipAddress(p.getIpAddress())
                        .bankCode(p.getBankCode())
                        .bankTranNo(p.getBankTranNo())
                        .cardType(p.getCardType())
                        .vnpayTxnNo(p.getVnpayTxnNo())
                        .paidAt(p.getPaidAt())
                        .createdAt(p.getCreatedAt())
                        .orderId(p.getOrderId())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CreatePaymentResponse createMonthlyPaymentByStore(
            String storeId, int month, HttpServletRequest httpRequest) {

        int year = LocalDate.now().getYear();

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<Order> orders = orderRepository
                .findByStoreIdAndOrderDateBetween(storeId, startOfMonth, endOfMonth);

        if (orders.isEmpty()) {
            throw new RuntimeException("Không có đơn hàng nào trong tháng " + month + " của store: " + storeId);
        }

        // ✅ Chỉ lấy order có PaymentOption = PAY_AT_THE_END_OF_MONTH
        List<Order> eligibleOrders = orders.stream()
                .filter(o -> PaymentOption.PAY_AT_THE_END_OF_MONTH.equals(o.getPaymentOption()))
                .collect(Collectors.toList());

        if (eligibleOrders.isEmpty()) {
            throw new RuntimeException("Không có đơn hàng nào có hình thức PAY_AT_THE_END_OF_MONTH trong tháng " + month);
        }

        // ✅ Lọc unpaidOrders từ eligibleOrders (không phải orders)
        List<Order> unpaidOrders = eligibleOrders.stream()
                .filter(o -> {
                    List<Payment> payments = paymentRepository.findByOrderId(o.getOrderId());
                    return payments.stream()
                            .noneMatch(p -> PaymentStatus.SUCCESS.equals(p.getStatus()));
                })
                .collect(Collectors.toList());

        if (unpaidOrders.isEmpty()) {
            throw new RuntimeException("Tất cả đơn hàng tháng " + month + " của store " + storeId + " đã được thanh toán");
        }

        // Tổng amount
        Long totalAmount = unpaidOrders.stream()
                .mapToLong(o -> o.getOrderDetail().getAmount().longValue())
                .sum();

        if (totalAmount <= 0) {
            throw new RuntimeException("Tổng giá trị đơn hàng không hợp lệ");
        }

        // ✅ Gom danh sách orderId
        List<String> orderIdList = unpaidOrders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        String orderIdsLog = String.join(",", orderIdList);

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
        vnpParams.put("vnp_OrderInfo", "Thanh toan thang " + month + " store " + storeId);
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
        payment.setStoreId(storeId);
        payment.setPaymentMonth(month);
        paymentRepository.save(payment);

        // Lưu PaymentRecord cho từng order
        unpaidOrders.forEach(o -> {
            PaymentRecord record = PaymentRecord.builder()
                    .orderId(o.getOrderId())
                    .txnRef(txnRef)
                    .amount(totalAmount)
                    .status("PENDING")
                    .build();
            paymentRecordRepository.save(record);
        });

        log.info("Tạo monthly payment: storeId={}, month={}, txnRef={}, totalAmount={}, orders={}",
                storeId, month, txnRef, totalAmount, orderIdsLog);

        return CreatePaymentResponse.builder()
                .txnRef(txnRef)
                .paymentUrl(paymentUrl)
                .amount(totalAmount)
                .orderIds(orderIdList) // ✅ trả về danh sách orderId được gộp
                .build();
    }

    public List<PaymentResponse> getPaymentsByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(p -> PaymentResponse.builder()
                        .id(p.getPaymentId())
                        .txnRef(p.getTxnRef())
                        .amount(p.getAmount())
                        .status(p.getStatus() != null ? p.getStatus().name() : null)
                        .responseCode(p.getResponseCode())
                        .ipAddress(p.getIpAddress())
                        .bankCode(p.getBankCode())
                        .bankTranNo(p.getBankTranNo())
                        .cardType(p.getCardType())
                        .vnpayTxnNo(p.getVnpayTxnNo())
                        .paidAt(p.getPaidAt())
                        .createdAt(p.getCreatedAt())
                        .orderId(p.getOrderId())
                        .build())
                .collect(Collectors.toList());
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

            // ── Cập nhật invoice ──────────────────────────────────
            String orderId = payment.getOrderId();
            orderInvoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
                invoice.setInvoiceStatus("PAID");
                invoice.setPaymentType("VNPAY");
                invoice.setTotalAmount(BigDecimal.valueOf(payment.getAmount()));
                invoice.setPaidDate(LocalDate.now());
                orderInvoiceRepository.save(invoice);
                log.info("Invoice updated to PAID for orderId: {}", orderId);
            });
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
