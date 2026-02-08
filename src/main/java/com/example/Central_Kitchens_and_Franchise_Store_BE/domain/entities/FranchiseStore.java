package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
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
    @Builder.Default
    private Set<FranchiseStorePaymentMethod> paymentMethods = new HashSet<>();

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<FranchiseStorePaymentMethod> paymentRecords = new HashSet<>();

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<FranchiseStoresFood> storeFoods = new HashSet<>();

    @OneToMany(mappedBy = "franchiseStore", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Order> orders = new HashSet<>();
}
