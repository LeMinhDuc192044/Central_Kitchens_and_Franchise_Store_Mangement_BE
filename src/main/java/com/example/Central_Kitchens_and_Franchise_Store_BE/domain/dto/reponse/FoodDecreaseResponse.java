package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDecreaseResponse {

    private String orderId;

    private List<FoodDecreaseDetail> decreasedFoods;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FoodDecreaseDetail {
        private String centralFoodId;
        private String foodName;
        private BigDecimal previousAmount;
        private BigDecimal decreasedBy;
        private BigDecimal remainingAmount;
    }
}
