package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.KitchenConfigResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdateConfigRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.KitchenConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Tag(name = "Kitchen Config", description = "Quản lý cấu hình Central Kitchen")
public class KitchenConfigController {

    private final KitchenConfigService kitchenConfigService;

    @GetMapping
    public ResponseEntity<List<KitchenConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(kitchenConfigService.getAllConfigs());
    }

    @GetMapping("/{key}")
    public ResponseEntity<KitchenConfigResponse> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(kitchenConfigService.getConfig(key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<KitchenConfigResponse> updateConfig(
            @PathVariable String key,
            @RequestBody @Valid UpdateConfigRequest request) {
        return ResponseEntity.ok(kitchenConfigService.updateConfig(key, request));
    }
}
