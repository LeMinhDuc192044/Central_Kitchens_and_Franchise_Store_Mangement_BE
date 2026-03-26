package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecordResponse(
        String paymentRecordId,
        String storeId,
        BigDecimal debtAmount,
        PaymentStatus status,
        LocalDateTime createdAt
) {}
