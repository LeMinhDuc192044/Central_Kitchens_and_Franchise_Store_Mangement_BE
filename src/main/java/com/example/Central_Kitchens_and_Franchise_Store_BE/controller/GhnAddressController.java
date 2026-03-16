package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;


import com.example.Central_Kitchens_and_Franchise_Store_BE.service.GhnAddressValidationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GhnAddressController {

    private final GhnAddressValidationService ghnAddressValidationService;

    @GetMapping("/provinces")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> getProvinces() {
        return ResponseEntity.ok(ghnAddressValidationService.getAvailableProvinces());
    }

    @GetMapping("/provinces/{provinceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> getProvinceById(@PathVariable Integer provinceId) {
        return ResponseEntity.ok(ghnAddressValidationService.getProvinceById(provinceId));
    }

    @GetMapping("/districts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ghnAddressValidationService.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLY_COORDINATOR', 'FRANCHISE_STAFF')")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ghnAddressValidationService.getAvailableWards(districtId));
    }
}