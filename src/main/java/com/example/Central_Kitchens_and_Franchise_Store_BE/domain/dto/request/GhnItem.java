package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GhnItem {
    private String name;      // required - product name
    private String code;      // optional - your product code/SKU
    private Integer quantity; // required
    private Integer price;    // required - price per item
    private Integer weight;   // grams per item
    private Integer length;
    private Integer width;
    private Integer height;
}
