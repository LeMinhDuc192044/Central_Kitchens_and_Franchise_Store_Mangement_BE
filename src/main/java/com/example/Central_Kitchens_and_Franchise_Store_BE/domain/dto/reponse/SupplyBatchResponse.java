package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyBatchResponse {

    private String batchId;
    private LocalDate batchDate;
    private BatchStatus status;
    private Integer totalItems;
    private Integer totalTypes;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private List<SupplyBatchItemResponse> items;
}