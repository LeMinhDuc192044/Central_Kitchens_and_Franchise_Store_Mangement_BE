package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInvoiceResponse {
    private String orderInvoiceId;
    private String invoiceStatus;
    private String paymentType;
    private BigDecimal totalAmount;
    private LocalDate paidDate;
    private String orderId;
}
