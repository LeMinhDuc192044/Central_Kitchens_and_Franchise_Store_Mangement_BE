package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String orderId;
    private Integer priorityLevel;
    private String note;
    private LocalDate orderDate;  // Có orderDate để hiển thị
    private String statusOrder;
    private String storeId;
}
