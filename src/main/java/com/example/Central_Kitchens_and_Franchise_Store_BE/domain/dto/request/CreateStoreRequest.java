package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateStoreRequest(
        String storeName,
        String address,
        String province,
        Integer district,
        String ward,
        @NotBlank
        @Pattern(
                regexp = "^0[0-9]{9}$",
                message = "Phone number must be 10 digits and start with 0"
        )
        String numberOfContact,
        Integer revenue
) {}
