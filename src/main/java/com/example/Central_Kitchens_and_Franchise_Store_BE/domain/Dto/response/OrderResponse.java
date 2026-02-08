package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.response;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderId;
    private Integer priorityLevel;
    private String note;
    private LocalDate orderDate;
    private OrderStatus statusOrder;
    private String storeId;
    private List<OrderDetailResponse> orderDetails;
}