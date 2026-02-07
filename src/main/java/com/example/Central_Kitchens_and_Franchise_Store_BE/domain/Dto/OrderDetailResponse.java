package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {

    private String orderDetailId;
    private BigDecimal amount;
    private String note;
    private List<OrderDetailItemResponse> items;
}