package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.KitchenConfigResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdateConfigRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.KitchenConfig;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.KitchenConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KitchenConfigService {

    private final KitchenConfigRepository kitchenConfigRepository;

    public List<KitchenConfigResponse> getAllConfigs() {
        return kitchenConfigRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public KitchenConfigResponse getConfig(String key) {
        return toResponse(findOrThrow(key));
    }

    @Transactional
    public KitchenConfigResponse updateConfig(String key, UpdateConfigRequest request) {
        KitchenConfig config = findOrThrow(key);

        config.setConfigValue(String.valueOf(request.getValue()));
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        return toResponse(kitchenConfigRepository.save(config));
    }

    private KitchenConfig findOrThrow(String key) {
        return kitchenConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Config không tồn tại: " + key));
    }

    private KitchenConfigResponse toResponse(KitchenConfig config) {
        return KitchenConfigResponse.builder()
                .configKey(config.getConfigKey())
                .configValue(config.getIntValue())
                .description(config.getDescription())
                .build();
    }
}
