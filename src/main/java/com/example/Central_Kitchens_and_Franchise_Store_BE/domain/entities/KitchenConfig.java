package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// domain/entities/KitchenConfig.java
@Entity
@Table(name = "kitchen_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "description")
    private String description;

    // Helper
    public int getIntValue() {
        return Integer.parseInt(configValue);
    }
}
