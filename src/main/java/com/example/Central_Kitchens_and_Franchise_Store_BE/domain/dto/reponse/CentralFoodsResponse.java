package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoodCategory;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Recipe;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralFoodsResponse {
    private String foodId;
    private String foodName;
    private BigDecimal amount;
    private LocalDate expiryDate;
    private LocalDate manufacturingDate;
    private FoodStatus centralFoodStatus;
    private Integer unitPriceFood;
    private Integer weight;           // grams
    private Integer length;           // cm
    private Integer width;            // cm
    private Integer height;
    private RecipeInfo recipe;
    private CentralFoodCategoryInfo centralFoodType;
}
