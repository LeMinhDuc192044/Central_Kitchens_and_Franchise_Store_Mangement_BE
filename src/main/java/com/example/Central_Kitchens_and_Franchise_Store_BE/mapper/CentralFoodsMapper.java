package com.example.Central_Kitchens_and_Franchise_Store_BE.mapper;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodCategoryInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodsResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.RecipeInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CentralFoodsMapper {

    public CentralFoodsResponse convertToDTO(CentralFoods food) {

        RecipeInfo recipeInfo = null;

        if (food.getRecipe() != null) {
            recipeInfo = RecipeInfo.builder()
                    .recipeId(food.getRecipe().getRecipeId())
                    .cookingTime(food.getRecipe().getCookingTime())
                    .cookingTemperature(food.getRecipe().getCookingTemperature())
                    .publishedDate(food.getRecipe().getPublishedDate())
                    .version(food.getRecipe().getVersion())
                    .materialUsageStandard(food.getRecipe().getMaterialUsageStandard())
                    .build();
        }

        CentralFoodCategoryInfo categoryInfo = null;

        if (food.getFoodType() != null) {
            categoryInfo = CentralFoodCategoryInfo.builder()
                    .CentralFoodCategoryId(food.getFoodType().getCentralFoodTypeId())
                    .CentralFoodCategoryName(food.getFoodType().getCentralFoodTypeName())
                    .build();
        }

        return CentralFoodsResponse.builder()
                .foodId(food.getCentralFoodId())
                .foodName(food.getFoodName())
                .centralFoodType(categoryInfo)
                .recipe(recipeInfo)
                .amount(food.getAmount())
                .weight(food.getWeight())
                .length(food.getLength())
                .width(food.getWidth())
                .height(food.getHeight())
                .expiryDate(food.getExpiryDate())
                .manufacturingDate(food.getManufacturingDate())
                .centralFoodStatus(food.getCentralFoodStatus())
                .unitPriceFood(food.getUnitPriceFood())
                .build();
    }

    public CentralFoods convertToEntity(CentralFoodsRequest dto) {
        return CentralFoods.builder()
                .foodName(dto.getFoodName())
                .weight(dto.getWeight())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .amount(dto.getAmount())
                .expiryDate(dto.getExpiryDate())
                .manufacturingDate(dto.getManufacturingDate())
                .centralFoodStatus(dto.getCentralFoodStatus())
                .unitPriceFood(dto.getUnitPriceFood())
                .build();
    }

    public List<CentralFoodsResponse> convertToDTOList(List<CentralFoods> foods) {
        if (foods == null) {
            return null;
        }

        return foods.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CentralFoods> convertToEntityList(List<CentralFoodsRequest> dtos) {
        if (dtos == null) {
            return null;
        }

        return dtos.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDTO(CentralFoods entity, CentralFoodsUpdateRequest dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setFoodName(dto.getFoodName());
        entity.setAmount(dto.getAmount());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setManufacturingDate(dto.getManufacturingDate());
        entity.setCentralFoodStatus(dto.getCentralFoodStatus());
        entity.setUnitPriceFood(dto.getUnitPriceFood());
    }
}
