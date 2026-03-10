package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {
    List<Ingredient> findByCategoryId(String categoryId);
}
