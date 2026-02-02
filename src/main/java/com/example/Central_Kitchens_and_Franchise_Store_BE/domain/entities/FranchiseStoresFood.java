package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "franchise_stores_food")
public class FranchiseStoresFood {

    @Id
    @Column(name = "food_id")
    private String foodId;

    @Column(name = "food_name")
    private String foodName;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "status_food")
    private String statusFood;

    @Column(name = "food_category_id_fk")
    private String foodCategoryId;

    @Column(name = "store_id_fk")
    private String storeId;

    @ManyToOne
    @JoinColumn(name = "food_category_id_fk", referencedColumnName = "food_category_id", insertable = false, updatable = false)
    private FranchiseStoresFoodCategory foodCategory;

    @ManyToOne
    @JoinColumn(name = "store_id_fk", referencedColumnName = "store_id", insertable = false, updatable = false)
    private FranchiseStore franchiseStore;
}
