package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodsResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.FoodDecreaseResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.FoodIncreaseResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.CentralFoodsMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.RandomGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CentralFoodsService {

    private final CentralFoodsRepository centralFoodsRepository;
    private final RecipeRepository recipeRepository;
    private final CentralFoodsRepository foodsRepository;
    private final CentralFoodsMapper centralFoodsMapper;
    private final CentralFoodCategoryRepository centralFoodCategoryRepository;
    private final OrderRepository orderRepository;
    private final RandomGeneratorUtil randomGeneratorUtil;
    private final SupplyBatchRepository supplyBatchRepository;


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

    @Transactional
    public FoodDecreaseResponse  decreaseFoodAmountByOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("" +
                        "Order with this " + orderId + " id does not existed!!!"));

        OrderDetail orderDetail = Optional.ofNullable(order.getOrderDetail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OrderDetail not found for Order ID: " + order.getOrderId()));

        List<OrderDetailItem> items = orderDetail.getOrderDetailItems();

        if (items == null || items.isEmpty()) {
            log.warn("No OrderDetailItems found for Order ID: {}", order.getOrderId());
            return FoodDecreaseResponse.builder()
                    .orderId(order.getOrderId())
                    .decreasedFoods(List.of())
                    .build();
        }

        List<FoodDecreaseResponse.FoodDecreaseDetail> details = new ArrayList<>();

        for (OrderDetailItem item : items) {
            String foodId = item.getCentralFoodId();
            BigDecimal quantityToDeduct = BigDecimal.valueOf(item.getQuantity());

            CentralFoods food = foodsRepository.findById(foodId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CentralFood not found with ID: " + foodId));

            if (food.getAmount().compareTo(quantityToDeduct) < 0) {
                throw new IllegalStateException(
                        "Insufficient stock for food [" + food.getFoodName() + "]. "
                                + "Available: " + food.getAmount()
                                + ", Requested: " + quantityToDeduct);
            }

            BigDecimal previousAmount = food.getAmount();
            BigDecimal remainingAmount = previousAmount.subtract(quantityToDeduct);

            food.setAmount(remainingAmount);
            foodsRepository.save(food);

            log.info("Decreased food [{}] amount by {}. Remaining: {}",
                    food.getFoodName(), quantityToDeduct, remainingAmount);

            details.add(FoodDecreaseResponse.FoodDecreaseDetail.builder()
                    .centralFoodId(food.getCentralFoodId())
                    .foodName(food.getFoodName())
                    .previousAmount(previousAmount)
                    .decreasedBy(quantityToDeduct)
                    .remainingAmount(remainingAmount)
                    .build());
        }

        return FoodDecreaseResponse.builder()
                .orderId(order.getOrderId())
                .decreasedFoods(details)
                .build();
    }

    @Transactional
    public FoodIncreaseResponse increaseFoodAmountByBatch(String batchId) {

        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Batch not found with ID: " + batchId));

        // Chỉ cho phép tăng tồn kho khi batch đã DELIVERED
        // (Central đã sản xuất xong và giao hàng thực tế)
        if (batch.getStatus() != BatchStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Chỉ có thể cập nhật tồn kho khi batch ở trạng thái DELIVERED. "
                            + "Trạng thái hiện tại: " + batch.getStatus());
        }

        List<SupplyBatchItem> items = batch.getItems();

        if (items == null || items.isEmpty()) {
            log.warn("No items found in batch ID: {}", batchId);
            return FoodIncreaseResponse.builder()
                    .batchId(batchId)
                    .increasedFoods(List.of())
                    .build();
        }

        List<FoodIncreaseResponse.FoodIncreaseDetail> details = new ArrayList<>();

        for (SupplyBatchItem item : items) {
            String foodId = item.getCentralFoodId();
            BigDecimal quantityToAdd = BigDecimal.valueOf(item.getTotalQuantity());

            CentralFoods food = foodsRepository.findById(foodId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CentralFood not found with ID: " + foodId));

            BigDecimal previousAmount = food.getAmount();
            BigDecimal newAmount = previousAmount.add(quantityToAdd);

            food.setAmount(newAmount);
            foodsRepository.save(food);

            log.info("Increased food [{}] amount by {}. New total: {}",
                    food.getFoodName(), quantityToAdd, newAmount);

            details.add(FoodIncreaseResponse.FoodIncreaseDetail.builder()
                    .centralFoodId(food.getCentralFoodId())
                    .foodName(food.getFoodName())
                    .previousAmount(previousAmount)
                    .increasedBy(quantityToAdd)
                    .remainingAmount(newAmount)
                    .build());
        }

        return FoodIncreaseResponse.builder()
                .batchId(batchId)
                .increasedFoods(details)
                .build();
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
        existingFood.setWeight(foodDTO.getWeight());
        existingFood.setHeight(foodDTO.getHeight());
        existingFood.setLength(foodDTO.getLength());
        existingFood.setWidth(foodDTO.getWidth());

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
