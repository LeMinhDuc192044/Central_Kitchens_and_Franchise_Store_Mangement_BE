package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipInvoiceInfo {
    private String shipInvoiceId;
    private BigDecimal totalPrice;
    private Integer paymentTypeId;
    private String paymentTypeName; // "Sender Pays" or "Receiver Pays"
}
