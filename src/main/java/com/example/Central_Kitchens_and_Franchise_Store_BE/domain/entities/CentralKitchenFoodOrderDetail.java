package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "central_kitchen_food_order_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralKitchenFoodOrderDetail {
    @Id
    @Column(name = "order_detail_id")
    private String orderDetailId;

    @Column(name = "central_food_id")
    private String centralFoodId;

    @ManyToOne
    @JoinColumn(name = "central_food_id", referencedColumnName = "central_food_id", insertable = false, updatable = false)
    private CentralFoods food;
}
