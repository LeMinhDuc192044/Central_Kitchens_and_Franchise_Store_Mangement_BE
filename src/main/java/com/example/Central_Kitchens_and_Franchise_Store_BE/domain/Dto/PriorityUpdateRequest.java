package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PriorityUpdateRequest {

    @NotNull(message = "Priority level is required")
    @Min(value = 1, message = "Priority level must be between 1 and 3")
    @Max(value = 3, message = "Priority level must be between 1 and 3")
    private Integer newPriority;

    private String note;
}
