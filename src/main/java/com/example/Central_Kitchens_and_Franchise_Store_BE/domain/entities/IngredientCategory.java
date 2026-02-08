package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ingredient_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientCategory {

    @Id
    @Column(name = "ingredient_type_id")
    private String ingredientTypeId;

    @Column(name = "ingredient_category")
    private String ingredientCategory;

    @OneToMany(mappedBy = "ingredientCategory", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Ingredient> ingredients = new HashSet<>();
}
