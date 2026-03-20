package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import java.util.List;

public record StoreResponse(
        String storeId,
        String storeName,
        String address,
        Integer district,
        String ward,
        String province,
        boolean deptStatus,
        String numberOfContact
) {}
