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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CentralFoodsUpdateRequest {
    private String foodName;

    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @PastOrPresent(message = "Manufacturing date cannot be in the future")
    private LocalDate manufacturingDate;

    private FoodStatus centralFoodStatus;

    @Positive(message = "Amount must be greater than 0")
    private BigDecimal unitPriceFood;

    private String recipeId;
    private String centralFoodTypeId;
}
