package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import java.math.BigDecimal;

public record CreatePaymentRecordRequest(
        String storeId,
        BigDecimal debtAmount   // 0 → deptStatus = false; >0 → deptStatus = true
) {}
