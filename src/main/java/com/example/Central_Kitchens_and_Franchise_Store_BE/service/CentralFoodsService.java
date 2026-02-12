package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodsResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoodCategory;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Recipe;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.CentralFoodsMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodCategoryRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodsRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.RecipeRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.utils.RandomGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CentralFoodsService {

    private final CentralFoodsRepository centralFoodsRepository;
    private final RecipeRepository recipeRepository;
    private final CentralFoodsRepository foodsRepository;
    private final CentralFoodsMapper centralFoodsMapper;
    private final CentralFoodCategoryRepository centralFoodCategoryRepository;
    private final RandomGeneratorUtil randomGeneratorUtil;


    public CentralFoodsResponse createFood(CentralFoodsRequest foodDTO) {

        CentralFoods food = centralFoodsMapper.convertToEntity(foodDTO);

        CentralFoodCategory centralFoodCategory = centralFoodCategoryRepository.findById(foodDTO.getCentralFoodTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Central food category with this "
                        + foodDTO.getRecipeId()
                        + " does snot exist"));
        food.setFoodType(centralFoodCategory);

        Recipe recipe = recipeRepository.findById(foodDTO.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipe with this "
                        + foodDTO.getRecipeId()
                        + " does snot exist"));
        food.setRecipe(recipe);
        food.setCentralFoodId(createCentralFoodsId(foodDTO.getCentralFoodTypeId()));

        CentralFoods savedFood = foodsRepository.save(food);
        return centralFoodsMapper.convertToDTO(savedFood);
    }

    @Transactional(readOnly = true)
    public CentralFoodsResponse getFoodById(String id) {
        CentralFoods food = foodsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
        return centralFoodsMapper.convertToDTO(food);
    }

    @Transactional(readOnly = true)
    public List<CentralFoodsResponse> getAllFoods() {
        return centralFoodsMapper.convertToDTOList(centralFoodsRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<CentralFoodsResponse> getFoodsByStatus(String status) {
        return centralFoodsMapper.convertToDTOList(foodsRepository.findByCentralFoodStatus(status));
    }

    @Transactional(readOnly = true)
    public List<CentralFoodsResponse> getExpiredFoods() {
        LocalDate today = LocalDate.now();
        return centralFoodsMapper.convertToDTOList(foodsRepository.findByExpiryDateBefore(today));
    }

    @Transactional(readOnly = true)
    public List<CentralFoodsResponse> getFoodsExpiringSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);
        return centralFoodsMapper.convertToDTOList(foodsRepository.findByExpiryDateBetween(today, futureDate));
    }

    public CentralFoodsResponse updateFood(String id, CentralFoodsUpdateRequest foodDTO) {
        CentralFoods existingFood = foodsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food not found with ID: " + id));


        if(!foodDTO.getCentralFoodTypeId().isEmpty()) {
            CentralFoodCategory centralFoodCategory = centralFoodCategoryRepository.findById(foodDTO.getCentralFoodTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Central food category with this "
                            + foodDTO.getRecipeId()
                            + " does snot exist"));
            existingFood.setFoodType(centralFoodCategory);

        }

        if(!foodDTO.getRecipeId().isEmpty()) {
            Recipe recipe = recipeRepository.findById(foodDTO.getRecipeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recipe with this "
                            + foodDTO.getRecipeId()
                            + " does snot exist"));
            existingFood.setRecipe(recipe);
        }

        existingFood.setFoodName(foodDTO.getFoodName());
        existingFood.setAmount(foodDTO.getAmount());
        existingFood.setExpiryDate(foodDTO.getExpiryDate());
        existingFood.setManufacturingDate(foodDTO.getManufacturingDate());
        existingFood.setCentralFoodStatus(foodDTO.getCentralFoodStatus());
        existingFood.setUnitPriceFood(foodDTO.getUnitPriceFood());

        CentralFoods updatedFood = foodsRepository.save(existingFood);
        return centralFoodsMapper.convertToDTO(updatedFood);
    }

    public void deleteFood(String id) {
        if (!foodsRepository.existsById(id)) {
            throw new RuntimeException("Food not found with ID: " + id);
        }
        foodsRepository.deleteById(id);
    }


    private String createCentralFoodsId(String centralFoodCategoryId) {
        String[] parts = centralFoodCategoryId.split("_");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid input format");
        }
        String foodType = parts[1];
        return "CE_" + foodType + "FO_" + randomGeneratorUtil.randomSix();
    }


}
