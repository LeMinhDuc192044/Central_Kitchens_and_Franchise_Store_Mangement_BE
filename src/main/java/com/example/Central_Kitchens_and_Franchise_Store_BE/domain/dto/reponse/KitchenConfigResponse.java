package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.Builder;
import lombok.Data;

// dto/response/KitchenConfigResponse.java
@Data
@Builder
public class KitchenConfigResponse {
    private String configKey;
    private int    configValue;
    private String description;
}
