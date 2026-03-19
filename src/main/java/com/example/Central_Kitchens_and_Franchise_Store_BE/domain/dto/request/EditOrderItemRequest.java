package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import lombok.Data;

@Data
public class EditOrderItemRequest {
    private String centralFoodId;  // food mới (hoặc giữ nguyên)
    private Integer quantity;
}
