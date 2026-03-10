package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.service.IngredientService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/ingredients")
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<IngredientService.IngredientResponse>> getAllIngredients() {
        return ResponseEntity.ok(ingredientService.getAllIngredients());
    }

    // GET /api/ingredients/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<IngredientService.IngredientResponse> getIngredientById(@PathVariable String id) {
        return ResponseEntity.ok(ingredientService.getIngredientById(id));
    }

    // GET /api/ingredients/category/{categoryId}
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<IngredientService.IngredientResponse>> getIngredientsByCategory(
            @PathVariable String categoryId) {
        return ResponseEntity.ok(ingredientService.getIngredientsByCategory(categoryId));
    }

    // PUT /api/ingredients/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTRAL_KITCHEN_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<IngredientService.IngredientResponse> updateIngredient(
            @PathVariable String id,
            @RequestBody IngredientService.IngredientUpdateRequest request) {
        return ResponseEntity.ok(ingredientService.updateIngredient(id, request));
    }
}
