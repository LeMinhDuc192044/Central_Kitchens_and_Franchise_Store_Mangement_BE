package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInvoicePaymentRequest {

    @NotBlank(message = "Payment type is required")
    private String paymentType; // e.g. "CASH", "BANK_TRANSFER"

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private String paymentRecordId; // optional nếu muốn gán vào record có sẵn
}
