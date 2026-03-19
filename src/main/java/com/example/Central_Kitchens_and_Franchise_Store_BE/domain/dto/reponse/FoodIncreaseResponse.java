package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodIncreaseResponse {

    private String batchId;
    private List<FoodIncreaseDetail> increasedFoods;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FoodIncreaseDetail {
        private String centralFoodId;
        private String foodName;
        private BigDecimal previousAmount;
        private BigDecimal increasedBy;
        private BigDecimal remainingAmount;
    }
}