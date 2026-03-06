package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeInfo {
    private String recipeId;
    private Integer cookingTime;
    private Double cookingTemperature;
    private LocalDate publishedDate;
    private String version;
    private String materialUsageStandard;

}
