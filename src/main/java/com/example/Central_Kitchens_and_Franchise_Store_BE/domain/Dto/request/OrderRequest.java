package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String note;

    @NotBlank(message = "Store ID is required")
    private String storeId;


    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderDetailRequest> orderDetails;
}