package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.InvoiceStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.ShipPaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipInvoiceResponse {
    private String shipInvoiceId;
    private String shipmentCodeId;
    private String ghnOrderCode;
    private BigDecimal totalPrice;
    private ShipPaymentType paymentType;
    private InvoiceStatus invoiceStatus;
    private LocalDateTime paidAt;
}
