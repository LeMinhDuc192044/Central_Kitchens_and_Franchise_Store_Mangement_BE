package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodStatus;
import jakarta.validation.constraints.*;
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
public class CentralFoodsRequest {

    @NotBlank(message = "Name must not be blank!!")
    private String foodName;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Expiry date must not be null")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @NotNull(message = "Manufacturing date must not be null")
    @PastOrPresent(message = "Manufacturing date cannot be in the future")
    private LocalDate manufacturingDate;

    @NotNull(message = "Food status must not be null")
    private FoodStatus centralFoodStatus;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be greater than 0")
    private Integer unitPriceFood;

    @NotBlank(message = "RecipeId must not be blank!!")
    private String recipeId;

    @NotNull(message = "Weight must not be blank!!!!")
    private Integer weight;           // grams

    @NotNull(message = "Length must not be blank!!!!")
    private Integer length;           // cm

    @NotNull(message = "Width must not be blank!!!!")
    private Integer width;            // cm

    @NotNull(message = "Height must not be blank!!!!")
    private Integer height;

    @NotBlank(message = "CentralFoodTypeId must not be blank!!")
    private String centralFoodTypeId;
}
