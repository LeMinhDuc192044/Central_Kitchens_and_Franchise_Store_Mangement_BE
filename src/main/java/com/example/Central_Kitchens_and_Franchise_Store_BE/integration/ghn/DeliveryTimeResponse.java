package com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeliveryTimeResponse {
    private String fromEstimateDate;     // raw from GHN
    private String toEstimateDate;       // raw from GHN
    private Long leadtime;               // unix timestamp

    // ── Human readable ────────────────────────────────────────────────────
    private String deliveryFrom;         // e.g. "21/03/2026 16:59"
    private String deliveryTo;           // e.g. "22/03/2026 16:59"
    private Long totalDays;              // e.g. 1
    private Long totalHours;             // e.g. 24
    private String summary;              // e.g. "Estimated delivery: 1-2 days (21/03 - 22/03/2026)"
}
