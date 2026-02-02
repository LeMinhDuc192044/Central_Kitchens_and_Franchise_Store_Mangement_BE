package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "franchise_store_payment_method")
public class FranchiseStorePaymentMethod {

    @Id
    @Column(name = "store_payment_id")
    private String storePaymentId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "store_id_fk")
    private String storeId;

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id", insertable = false, updatable = false)
    private FranchiseStore franchiseStore;
}