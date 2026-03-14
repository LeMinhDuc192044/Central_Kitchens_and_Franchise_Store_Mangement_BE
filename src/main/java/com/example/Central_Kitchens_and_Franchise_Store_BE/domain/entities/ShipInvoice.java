package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.InvoiceStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipPaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ship_invoice")
public class ShipInvoice {

    @Id
    @Column(name = "ship_invoice_id")
    private String shipInvoiceId;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private ShipPaymentType paymentType;

    @Column(name = "shipment_code_id_fk")
    private String shipmentCodeId;

    @Column(name = "invoice_status")
    @Enumerated(EnumType.STRING)
    private InvoiceStatus invoiceStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToOne
    @JoinColumn(name = "shipment_code_id_fk", referencedColumnName = "shipment_code_id", insertable = false, updatable = false)
    private Shipment shipment;
}