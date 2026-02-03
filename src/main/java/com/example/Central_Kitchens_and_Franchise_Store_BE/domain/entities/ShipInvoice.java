package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
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
    private String paymentType;

    @Column(name = "travel_distance")
    private Double travelDistance;

    @Column(name = "shipment_code_id_fk")
    private String shipmentCodeId;

    @OneToOne
    @JoinColumn(name = "shipment_code_id_fk", referencedColumnName = "shipment_code_id", insertable = false, updatable = false)
    private Shipment shipment;
}