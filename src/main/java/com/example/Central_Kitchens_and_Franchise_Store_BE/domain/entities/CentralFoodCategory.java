package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "central_food_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralFoodCategory {
    @Id
    @Column(name = "central_food_type_id")
    private String centralFoodTypeId;

    @Column(name = "central_food_type_name")
    private String centralFoodTypeName;

    @OneToMany(mappedBy = "foodType", cascade = CascadeType.ALL)
    private Set<CentralFoods> foods;
}
