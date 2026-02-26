package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "payment_id")
    private String paymentId;

    // Mã giao dịch merchant tự tạo (gửi lên VNPay là vnp_TxnRef)
    // VNPay trả về cái này trong callback để bạn tìm lại giao dịch
    @Column(name = "txn_ref", unique = true, nullable = false)
    private String txnRef;

    // Số tiền VNĐ (lưu số gốc, không nhân 100)
    // Ví dụ: 150000 = 150,000 VNĐ
    @Column(name = "amount", nullable = false)
    private Long amount;

    // Trạng thái giao dịch: PENDING / SUCCESS / FAILED / CANCELLED
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // Mã ngân hàng user chọn: "NCB", "VISA", "MASTERCARD"...
    // VNPay trả về sau khi thanh toán
    @Column(name = "bank_code")
    private String bankCode;

    // Mã giao dịch phía ngân hàng (dùng tra soát khi có tranh chấp)
    @Column(name = "bank_tran_no")
    private String bankTranNo;

    // Loại thẻ: "ATM", "CREDIT", "QRCODE"
    @Column(name = "card_type")
    private String cardType;

    // Mã giao dịch do VNPay tạo ra (khác txnRef do bạn tạo)
    // Cần khi liên hệ VNPay hỗ trợ
    @Column(name = "vnpay_txn_no")
    private String vnpayTxnNo;

    // Mã kết quả VNPay: "00"=thành công, "24"=huỷ, "51"=không đủ tiền...
    @Column(name = "response_code")
    private String responseCode;

    // IP của user lúc tạo giao dịch (VNPay bắt buộc, dùng chống gian lận)
    @Column(name = "ip_address")
    private String ipAddress;

    // Thời điểm tạo giao dịch (auto set)
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Thời điểm thanh toán thành công (null nếu chưa thanh toán)
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // FK liên kết với Order
    @Column(name = "order_id_fk")
    private String orderId;

    @ManyToOne
    @JoinColumn(name = "order_id_fk", referencedColumnName = "order_id", insertable = false, updatable = false)
    private Order order;

    @PrePersist
    void prePersist() {
        // Tự generate ID nếu chưa có
        if (this.paymentId == null) {
            this.paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            // Ví dụ: PAY-A1B2C3D4
        }
        this.status = PaymentStatus.PENDING;
    }
}