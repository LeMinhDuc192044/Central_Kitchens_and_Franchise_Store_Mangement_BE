package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnWebhookPayload {
    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("ExDescription")
    private String exDescription;   // reason for failure if any

    @JsonProperty("Time")
    private String time;

    @JsonProperty("CODAmount")
    private Integer codAmount;

    @JsonProperty("CODTransferDate")
    private String codTransferDate;

    @JsonProperty("Reason")
    private String reason;
}
