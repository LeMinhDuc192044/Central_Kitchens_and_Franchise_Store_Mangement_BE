package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import jakarta.persistence.Access;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MonthlyOrderResponse {
    private int    month;
    private int    year;
    private String storeId;       // null if all stores
    private long   totalOrders;
    private List<OrderResponse> orders;
}
