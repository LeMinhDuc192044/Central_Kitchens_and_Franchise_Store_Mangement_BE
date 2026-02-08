package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.response;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodItem;
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

    private FoodItem foodItem;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

}