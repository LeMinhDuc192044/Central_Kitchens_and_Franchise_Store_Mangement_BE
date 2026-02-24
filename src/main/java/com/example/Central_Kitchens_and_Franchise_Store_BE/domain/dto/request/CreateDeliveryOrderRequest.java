package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateDeliveryOrderRequest {

    @NotNull
    @Min(1)
    @Max(2)
    private Integer payment_type_id;   // 1=sender pays, 2=receiver pays
    private String note;
    private RequiredNote required_note;     // CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG

    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private Integer to_district_id;
    private Integer cod_amount;       // Cash on delivery amount
    private Integer weight;           // grams
    private Integer length;           // cm
    private Integer width;            // cm
    private Integer height;           // cm

    @NotNull
    @Min(1)
    @Max(2)
    private Integer service_type_id;  // 1=Express, 2=Standard

    private String client_order_code; // your internal order ID

    private List<GhnItem> items;
}
