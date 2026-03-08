package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import java.util.List;

public record StoreResponse(
        String storeId,
        String storeName,
        String address,
        String district,
        String ward,
        boolean deptStatus,
        Integer revenue,
        String numberOfContact,
        String managerEmail,
        List<String> paymentMethods
) {}
