package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "franchise_store")
public class FranchiseStore {

    @Id
    @Column(name = "store_id")
    private String storeId;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "address")
    private String address;

    @Column(name = "dept_status")
    private String deptStatus;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "number_of_contact")
    private String numberOfContact;

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    private Set<FranchiseStorePaymentMethod> paymentMethods;

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    private Set<FranchiseStorePaymentMethod> paymentRecords;

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    private Set<FranchiseStoresFood> storeFoods;

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    private Set<Order> orders;
}
