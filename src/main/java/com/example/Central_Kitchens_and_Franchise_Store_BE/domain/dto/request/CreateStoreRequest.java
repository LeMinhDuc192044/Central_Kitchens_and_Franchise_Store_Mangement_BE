package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

public record CreateStoreRequest(
        String storeName,
        String address,
        String district,
        String ward,
        Integer revenue
) {}
