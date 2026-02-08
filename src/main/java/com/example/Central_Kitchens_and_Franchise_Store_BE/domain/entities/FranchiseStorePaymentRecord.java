package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "franchise_store_payment_record")
public class FranchiseStorePaymentRecord {

    @Id
    @Column(name = "payment_record_id")
    private String paymentRecordId;

    @Column(name = "debt_amount")
    private BigDecimal debtAmount;

    @Column(name = "store_id_fk")
    private String storeId;

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id", insertable = false, updatable = false)
    private FranchiseStore franchiseStore;
}
