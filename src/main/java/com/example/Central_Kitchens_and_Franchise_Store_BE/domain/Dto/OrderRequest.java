package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String orderId;
    private String note;
    private String storeId;

}
