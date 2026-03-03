package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailItemResponse {

    private String centralFoodId;  // ← thay FoodItem foodItem
    private String foodName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

}