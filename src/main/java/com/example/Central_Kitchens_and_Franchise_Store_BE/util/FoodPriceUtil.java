package com.example.Central_Kitchens_and_Franchise_Store_BE.util;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodItem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class FoodPriceUtil {

    private static final Map<String, BigDecimal> FOOD_PRICES = new HashMap<>();

    static {
        // Gà
        FOOD_PRICES.put("GA_PHO_MAI", new BigDecimal("75000"));
        FOOD_PRICES.put("GA_TOI_OT", new BigDecimal("70000"));
        FOOD_PRICES.put("GA_SOT_TERIYAKI", new BigDecimal("80000"));

        // Mỳ Ý
        FOOD_PRICES.put("MY_Y_BO", new BigDecimal("65000"));
        FOOD_PRICES.put("MY_Y_CUA", new BigDecimal("85000"));
        FOOD_PRICES.put("MY_Y_TOM", new BigDecimal("90000"));

        // Bánh Kem
        FOOD_PRICES.put("BANH_KEM_VANILLA", new BigDecimal("250000"));
        FOOD_PRICES.put("BANH_KEM_CHOCOLATE", new BigDecimal("280000"));
        FOOD_PRICES.put("BANH_KEM_DAU", new BigDecimal("260000"));

        // Burger
        FOOD_PRICES.put("BURGER_BO", new BigDecimal("55000"));
        FOOD_PRICES.put("BURGER_GA", new BigDecimal("50000"));
        FOOD_PRICES.put("BURGER_PHO_MAI", new BigDecimal("60000"));
    }

    /**
     * Lấy giá của món ăn theo FoodItem enum
     * @param foodItem món ăn cần lấy giá
     * @return giá của món ăn, trả về 0 nếu không tìm thấy
     */
    public static BigDecimal getPrice(FoodItem foodItem) {
        return FOOD_PRICES.getOrDefault(foodItem.name(), BigDecimal.ZERO);
    }

    /**
     * Tính tổng tiền dựa trên món ăn và số lượng
     * @param foodItem món ăn
     * @param quantity số lượng
     * @return tổng tiền
     */
    public static BigDecimal calculateAmount(FoodItem foodItem, Integer quantity) {
        BigDecimal price = getPrice(foodItem);
        return price.multiply(new BigDecimal(quantity));
    }

    /**
     * Kiểm tra xem món ăn có tồn tại giá hay không
     * @param foodItem món ăn cần kiểm tra
     * @return true nếu có giá, false nếu không
     */
    public static boolean hasPrice(FoodItem foodItem) {
        return FOOD_PRICES.containsKey(foodItem.name());
    }
}