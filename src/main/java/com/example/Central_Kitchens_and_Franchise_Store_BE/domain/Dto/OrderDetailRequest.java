package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailRequest {


    private String note;
    @NotEmpty(message = "OrderDetail must have at least one item")
    @Valid
    private List<OrderDetailItemRequest> items;

}