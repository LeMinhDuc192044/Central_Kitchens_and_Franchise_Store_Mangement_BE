package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentInfo {

    private String shipmentCodeId;
    private String note;
    private RequiredNote requiredNote;
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;
    private Integer toDistrictId;
    private Integer codAmount;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer serviceTypeId;
    private String clientOrderCode;
    private String ghnOrderCode;
    private String orderDetailId;

    private ShipInvoiceInfo shipInvoice;
}
