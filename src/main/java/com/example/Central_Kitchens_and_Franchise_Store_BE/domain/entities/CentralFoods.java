package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;


import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "central_foods")
@Getter
@Setter
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
    @PositiveOrZero
    private BigDecimal amount;

    @Column(name = "expiry_date")
    @Future
    private LocalDate expiryDate;

    @Column(name = "manufacturing_date")
    @PastOrPresent
    private LocalDate manufacturingDate;

    @Column(name = "central_food_status")
    @Enumerated(EnumType.STRING)
    private FoodStatus centralFoodStatus;

    @Column(name = "unit_price_food")
    @Positive
    private Integer unitPriceFood;

    @Column(name = "food_weight")
    @Positive
    private Integer weight;

    @Column(name = "food_length")
    @Positive
    private Integer length;

    @Column(name = "food_width")
    @Positive
    private Integer width;

    @Column(name = "food_height")
    @Positive
    private Integer height;

    @ManyToOne
    @JoinColumn(name = "recipe_id", referencedColumnName = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @Column(name = "central_food_type_id")
    private String centralFoodTypeId;

    @ManyToOne
    @JoinColumn(name = "central_food_type_id", referencedColumnName = "central_food_type_id", insertable = false, updatable = false)
    private CentralFoodCategory foodType;

    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<CentralKitchenFoodOrderDetail> orderDetails = new HashSet<>();
}
