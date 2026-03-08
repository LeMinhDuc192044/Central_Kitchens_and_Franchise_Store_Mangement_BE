package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecordResponse(
        String paymentRecordId,
        String storeId,
        BigDecimal debtAmount,
        String status,
        LocalDateTime createdAt
) {}
