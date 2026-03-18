package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FranchiseStoreInfo {
    private String storeId;
    private String storeName;
    private String address;
    private boolean deptStatus;
    private Integer district;
    private String ward;
    private String numberOfContact;
}
