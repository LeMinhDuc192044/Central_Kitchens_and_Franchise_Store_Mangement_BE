package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums;

import lombok.Getter;

@Getter
public enum FoodItem {
    // Gà
    GA_PHO_MAI("Gà Phô Mai", "CHICKEN"),
    GA_TOI_OT("Gà Tỏi Ớt", "CHICKEN"),
    GA_SOT_TERIYAKI("Gà Sốt Teriyaki", "CHICKEN"),

    // Mỳ Ý
    MY_Y_BO("Mỳ Ý Bò", "PASTA"),
    MY_Y_CUA("Mỳ Ý Cua", "PASTA"),
    MY_Y_TOM("Mỳ Ý Tôm", "PASTA"),

    // Bánh Kem
    BANH_KEM_VANILLA("Bánh Kem Vanilla", "CAKE"),
    BANH_KEM_CHOCOLATE("Bánh Kem Chocolate", "CAKE"),
    BANH_KEM_DAU("Bánh Kem Dâu", "CAKE"),

    // Burger
    BURGER_BO("Burger Bò", "BURGER"),
    BURGER_GA("Burger Gà", "BURGER"),
    BURGER_PHO_MAI("Burger Phô Mai", "BURGER");

    private final String displayName;
    private final String category;

    FoodItem(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }
}