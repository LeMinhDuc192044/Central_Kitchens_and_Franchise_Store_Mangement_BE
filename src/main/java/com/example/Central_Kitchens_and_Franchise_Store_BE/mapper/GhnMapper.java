package com.example.Central_Kitchens_and_Franchise_Store_BE.mapper;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GhnMapper {


    public List<GhnItem> convertToGhnItems(
            Map<String, Integer> foods,
            List<CentralFoods> centralFoodsList) {

        Map<String, CentralFoods> foodMap = centralFoodsList.stream()
                .collect(Collectors.toMap(CentralFoods::getCentralFoodId, f -> f));

        return foods.entrySet().stream()
                .map(entry -> {
                    CentralFoods food = foodMap.get(entry.getKey());

                    if (food == null) {
                        throw new RuntimeException("Food not found: " + entry.getKey());
                    }

                    return GhnItem.builder()
                            .name(food.getFoodName())
                            .code(food.getCentralFoodId())
                            .price(food.getUnitPriceFood())
                            .quantity(entry.getValue())
                            .weight(food.getWeight())
                            .length(food.getLength())
                            .width(food.getWidth())
                            .height(food.getHeight())
                            .build();
                })
                .toList();
    }

}
