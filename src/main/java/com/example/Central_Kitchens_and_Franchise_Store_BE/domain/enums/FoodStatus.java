package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums;

public enum FoodStatus {
    AVAILABLE,        // Can be sold / used
    OUT_OF_STOCK,     // Quantity = 0
    EXPIRED,          // Past expiry date
    NEAR_EXPIRY,      // Close to expiry (warning)
    RESERVED,         // Reserved for orders
    DAMAGED,          // Broken / contaminated
    DISCONTINUED
}
