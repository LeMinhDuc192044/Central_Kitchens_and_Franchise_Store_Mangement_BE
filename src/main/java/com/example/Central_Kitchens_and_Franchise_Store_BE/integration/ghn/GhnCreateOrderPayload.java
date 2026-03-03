package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.RequiredNote;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GhnCreateOrderPayload {

    private Integer payment_type_id;
    private String note;
    private RequiredNote required_note;
    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private Integer to_district_id;
    private Integer cod_amount;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer service_type_id;
    private String client_order_code; // ← internal only, set by service
    private List<GhnItem> items;
}
