package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String orderId;
    private Integer priorityLevel;
    private String note;
    private String statusOrder;
    private String storeId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;
}
