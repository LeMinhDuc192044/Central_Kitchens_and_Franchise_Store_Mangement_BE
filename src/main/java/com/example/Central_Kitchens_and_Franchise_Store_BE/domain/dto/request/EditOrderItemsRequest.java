package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class EditOrderItemsRequest {
    private List<EditOrderItemRequest> items;
}
