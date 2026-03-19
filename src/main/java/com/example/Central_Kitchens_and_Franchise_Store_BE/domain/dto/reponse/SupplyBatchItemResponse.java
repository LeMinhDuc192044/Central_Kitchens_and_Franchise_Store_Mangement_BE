package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyBatchItemResponse {

    private String itemId;
    private String centralFoodId;
    private String foodName;
    private Integer totalQuantity;
    private String sourceDetail;   // "STORE-A: 20, STORE-B: 15"
}