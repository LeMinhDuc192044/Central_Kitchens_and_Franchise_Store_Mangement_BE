package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums;

public enum IngredientStatus {
    AVAILABLE,      // Ingredient is usable and in stock
    LOW_STOCK,      // Quantity is below safety threshold
    OUT_OF_STOCK,   // No quantity left
    EXPIRED,        // Passed expiration date
    RESERVED,       // Allocated for production/order
    DAMAGED,        // Spoiled or damaged during storage
    DISCARDED       // Removed from inventory
}
