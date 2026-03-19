package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatePreviewResponse {

    private Integer totalTypes;
    private Integer totalQuantity;
    private Integer estimatedBatchCount;
    private String warning;
    private List<AggregatedFoodItem> aggregatedItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AggregatedFoodItem {
        private String centralFoodId;
        private String foodName;
        private Integer totalQuantity;
        private String sourceDetail;
    }
}