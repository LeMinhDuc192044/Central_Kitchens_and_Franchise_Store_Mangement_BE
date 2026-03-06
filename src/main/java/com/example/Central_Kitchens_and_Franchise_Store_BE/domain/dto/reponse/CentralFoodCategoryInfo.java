package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralFoodCategoryInfo {

    private String CentralFoodCategoryId;
    private String CentralFoodCategoryName;

}
