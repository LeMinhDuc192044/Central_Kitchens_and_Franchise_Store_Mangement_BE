package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodsResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsUpdateRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.CentralFoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/central_foods")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Central Foods", description = "Central Foods APIs")
public class CentralFoodsController {

    private final CentralFoodsService centralFoodsService;

    @Operation(summary = "Create new food product", description = "Add a new food product to the central kitchen inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF')")
    @PostMapping
    public ResponseEntity<CentralFoodsResponse> createFood(
            @Parameter(description = "Food product details", required = true)
            @RequestBody CentralFoodsRequest foodDTO) {
        CentralFoodsResponse createdFood = centralFoodsService.createFood(foodDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFood);
    }

    @Operation(summary = "Get all food products", description = "Retrieve a list of all food products in the central kitchen")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF', 'SUPPLY_COORDINATOR')")
    @GetMapping
    public ResponseEntity<List<CentralFoodsResponse>> getAllFoods() {
        List<CentralFoodsResponse> foods = centralFoodsService.getAllFoods();
        return ResponseEntity.ok(foods);
    }

    @Operation(summary = "Get food by ID", description = "Retrieve a specific food product by its ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF', 'SUPPLY_COORDINATOR')")
    @GetMapping("/{id}")
    public ResponseEntity<CentralFoodsResponse> getFoodById(
            @Parameter(description = "Food ID", required = true, example = "FOOD001")
            @PathVariable String id) {
        CentralFoodsResponse food = centralFoodsService.getFoodById(id);
        return ResponseEntity.ok(food);
    }

    @Operation(summary = "Get foods by status", description = "Retrieve food products filtered by their status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF', 'SUPPLY_COORDINATOR')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CentralFoodsResponse>> getFoodsByStatus(
            @Parameter(description = "Food status", required = true, example = "Available")
            @PathVariable String status) {
        List<CentralFoodsResponse> foods = centralFoodsService.getFoodsByStatus(status);
        return ResponseEntity.ok(foods);
    }

    @Operation(summary = "Get expired foods", description = "Retrieve all food products that have expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF', 'SUPPLY_COORDINATOR')")
    @GetMapping("/expired")
    public ResponseEntity<List<CentralFoodsResponse>> getExpiredFoods() {
        List<CentralFoodsResponse> foods = centralFoodsService.getExpiredFoods();
        return ResponseEntity.ok(foods);
    }

    @Operation(summary = "Get foods expiring soon", description = "Retrieve food products expiring within specified days")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF', 'SUPPLY_COORDINATOR')")
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<CentralFoodsResponse>> getFoodsExpiringSoon(
            @Parameter(description = "Number of days", required = true, example = "7")
            @RequestParam(defaultValue = "7") int days) {
        List<CentralFoodsResponse> foods = centralFoodsService.getFoodsExpiringSoon(days);
        return ResponseEntity.ok(foods);
    }

    @Operation(summary = "Update food product", description = "Update an existing food product by its ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<CentralFoodsResponse> updateFood(
            @Parameter(description = "Food ID", required = true, example = "FOOD001")
            @PathVariable String id,
            @Parameter(description = "Updated food details", required = true)
            @RequestBody CentralFoodsUpdateRequest foodDTO) {
        CentralFoodsResponse updatedFood = centralFoodsService.updateFood(id, foodDTO);
        return ResponseEntity.ok(updatedFood);
    }

    @Operation(summary = "Delete food product", description = "Delete a food product by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Food deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Food not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTRAL_KITCHEN_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFood(
            @Parameter(description = "Food ID", required = true, example = "FOOD001")
            @PathVariable String id) {
        centralFoodsService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }
}
