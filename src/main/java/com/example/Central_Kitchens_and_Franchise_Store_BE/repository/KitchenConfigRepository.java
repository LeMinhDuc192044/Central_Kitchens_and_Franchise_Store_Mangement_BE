package com.example.Central_Kitchens_and_Franchise_Store_BE.repository;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.KitchenConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// repository/KitchenConfigRepository.java
public interface KitchenConfigRepository extends JpaRepository<KitchenConfig, String> {
    Optional<KitchenConfig> findByConfigKey(String configKey);
}
