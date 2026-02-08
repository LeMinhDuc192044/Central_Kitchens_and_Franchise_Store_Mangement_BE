package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @Column(name = "recipe_id")
    private String recipeId;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @Column(name = "cooking_temperature")
    private Double cookingTemperature;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "version")
    private String version;

    @Column(name = "material_usage_standard")
    private String materialUsageStandard;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<RecipeIngredient> recipeIngredients = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<CentralFoods> foods = new HashSet<>();
}
