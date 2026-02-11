package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "franchise_stores_food_category")
public class FranchiseStoresFoodCategory {

    @Id
    @Column(name = "food_category_id")
    private String foodCategoryId;

    @Column(name = "food_type_name")
    private String foodTypeName;

    @OneToMany(mappedBy = "foodCategory", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<FranchiseStoresFood> foods = new HashSet<>();
}
