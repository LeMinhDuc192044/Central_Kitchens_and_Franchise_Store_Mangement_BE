package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "central_foods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralFoods {
    @Id
    @Column(name = "central_food_id")
    private String centralFoodId;

    @Column(name = "food_name")
    private String foodName;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "central_food_status")
    private String centralFoodStatus;

    @Column(name = "unit_price_food")
    private BigDecimal unitPriceFood;

    @Column(name = "recipe_id")
    private String recipeId;

    @Column(name = "central_food_type_id")
    private String centralFoodTypeId;

    @ManyToOne
    @JoinColumn(name = "recipe_id", referencedColumnName = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "central_food_type_id", referencedColumnName = "central_food_type_id", insertable = false, updatable = false)
    private CentralFoodCategory foodType;

    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL)
    private Set<CentralKitchenFoodOrderDetail> orderDetails;
}
