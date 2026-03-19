package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// CreateBatchRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBatchRequest {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate batchDate;

    private String note;

    @NotEmpty(message = "Batch phải có ít nhất 1 item")
    private List<BatchItemRequest> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BatchItemRequest {

        @NotBlank
        private String centralFoodId;

        @NotNull
        @Min(value = 1)
        private Integer quantity;
    }
}
