package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_invoice")
public class OrderInvoice {

    @Id
    @Column(name = "order_invoice_id")
    private String orderInvoiceId;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "invoice_status")
    private String invoiceStatus;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_record_id_fk")
    private String paymentRecordId;

    @Column(name = "order_id_fk")
    private String orderId;

    @OneToOne
    @JoinColumn(name = "order_id_fk", referencedColumnName = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "payment_record_id_fk", referencedColumnName = "payment_record_id", insertable = false, updatable = false)
    private FranchiseStorePaymentRecord paymentRecord;
}