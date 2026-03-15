package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;


import com.example.Central_Kitchens_and_Franchise_Store_BE.service.GhnAddressValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class GhnAddressController {

    private final GhnAddressValidationService ghnAddressValidationService;

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return ResponseEntity.ok(ghnAddressValidationService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ghnAddressValidationService.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ghnAddressValidationService.getWards(districtId));
    }
}