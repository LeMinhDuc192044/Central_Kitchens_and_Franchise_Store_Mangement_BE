package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "recipe")
@Data
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
    private Set<RecipeIngredient> recipeIngredients;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private Set<Food> foods;
}
