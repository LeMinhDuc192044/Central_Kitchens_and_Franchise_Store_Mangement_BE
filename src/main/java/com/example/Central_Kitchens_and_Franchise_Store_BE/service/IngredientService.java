package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Ingredient;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.IngredientStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.IngredientRepository;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients() {
        return ingredientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public IngredientResponse getIngredientById(String id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with ID: " + id));
        return toResponse(ingredient);
    }

    // GET BY CATEGORY
    @Transactional(readOnly = true)
    public List<IngredientResponse> getIngredientsByCategory(String categoryId) {
        return ingredientRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // UPDATE
    @Transactional
    public IngredientResponse updateIngredient(String id, IngredientUpdateRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with ID: " + id));

        ingredient.setIngredientName(request.getIngredientName());
        ingredient.setQuantity(request.getQuantity());
        ingredient.setExpireDate(request.getExpireDate());
        ingredient.setManufacturingDate(request.getManufacturingDate());
        ingredient.setImportDate(request.getImportDate());
        ingredient.setStatus(request.getStatus());
        ingredient.setOrigin(request.getOrigin());
        ingredient.setUnitPrice(request.getUnitPrice());
        ingredient.setCategoryId(request.getCategoryId());

        return toResponse(ingredientRepository.save(ingredient));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private IngredientResponse toResponse(Ingredient i) {
        return IngredientResponse.builder()
                .ingredientId(i.getIngredientId())
                .ingredientName(i.getIngredientName())
                .quantity(i.getQuantity())
                .expireDate(i.getExpireDate())
                .manufacturingDate(i.getManufacturingDate())
                .importDate(i.getImportDate())
                .status(i.getStatus())
                .origin(i.getOrigin())
                .unitPrice(i.getUnitPrice())
                .categoryId(i.getCategoryId())
                .categoryName(i.getIngredientCategory() != null
                        ? i.getIngredientCategory().getIngredientCategory() : null)
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientUpdateRequest {
        private String ingredientName;
        private BigDecimal quantity;
        private LocalDate expireDate;
        private LocalDate manufacturingDate;
        private LocalDate importDate;
        private IngredientStatus status;
        private String origin;
        private BigDecimal unitPrice;
        private String categoryId;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientResponse {
        private String ingredientId;
        private String ingredientName;
        private BigDecimal quantity;
        private LocalDate expireDate;
        private LocalDate manufacturingDate;
        private LocalDate importDate;
        private IngredientStatus status;
        private String origin;
        private BigDecimal unitPrice;
        private String categoryId;
        private String categoryName;  // from IngredientCategory
    }
}
