package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.GhnConfig;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnAddressValidationService {

    private final RestTemplate restTemplate;
    private final GhnConfig ghnConfig;

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        return headers;
    }

    // ── GET ALL PROVINCES ──────────────────────────────────────────────────
    public List<Map<String, Object>> getProvinces() {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/province";
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return (List<Map<String, Object>>) response.getBody().get("data");
    }

    public List<Map<String, Object>> getAvailableProvinces() {
        List<Map<String, Object>> allProvinces = getProvinces();

        return allProvinces.stream()
                .filter(province -> {
                    Integer provinceId = (Integer) province.get("ProvinceID");
                    try {
                        // Check if province has at least one available district
                        List<Map<String, Object>> districts = getDistricts(provinceId);
                        return districts != null && !districts.isEmpty();
                    } catch (Exception e) {
                        log.warn("Province [{}] has no available districts, filtering out",
                                province.get("ProvinceName"));
                        return false;
                    }
                })
                .toList();
    }

    // ── GET ONLY AVAILABLE WARDS (not blocked by GHN) ─────────────────────────
    public List<Map<String, Object>> getAvailableWards(Integer districtId) {
        String url = ghnConfig.getBaseUrl() +
                "/shiip/public-api/master-data/ward?district_id=" + districtId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> body = response.getBody();

            if (body == null || !Integer.valueOf(200).equals(body.get("code"))) {
                log.warn("No available wards for districtId={}", districtId);
                return List.of();
            }

            List<Map<String, Object>> wards = (List<Map<String, Object>>) body.get("data");
            return wards != null ? wards : List.of();

        } catch (Exception e) {
            log.warn("Failed to get wards for districtId={}: {}", districtId, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getProvinceById(Integer provinceId) {
        List<Map<String, Object>> provinces = getProvinces();

        return provinces.stream()
                .filter(p -> provinceId.equals(p.get("ProvinceID")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Province not found with ID: " + provinceId +
                                ". Use GET /api/address/provinces to get valid IDs."));
    }

    // ── GET DISTRICTS BY PROVINCE ID ──────────────────────────────────────
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/district";

        Map<String, Object> body = Map.of("province_id", provinceId);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return (List<Map<String, Object>>) response.getBody().get("data");
    }

    // ── GET WARDS BY DISTRICT ID ───────────────────────────────────────────
    public List<Map<String, Object>> getWards(Integer districtId) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/ward";

        Map<String, Object> body = Map.of("district_id", districtId);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return (List<Map<String, Object>>) response.getBody().get("data");
    }

    // ── VALIDATE DISTRICT NAME → RETURN DISTRICT ID ───────────────────────
    public Integer validateAndGetDistrictId(Integer provinceId, String districtName) {
        List<Map<String, Object>> districts = getDistricts(provinceId);

        return districts.stream()
                .filter(d -> {
                    String name = (String) d.get("DistrictName");
                    return name != null && name.equalsIgnoreCase(districtName.trim());
                })
                .map(d -> (Integer) d.get("DistrictID"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "District [" + districtName + "] not found in GHN. " +
                                "Please use exact GHN district name."));
    }

    // ── VALIDATE WARD NAME → RETURN WARD CODE ─────────────────────────────
    public String validateAndGetWardCode(Integer districtId, String wardName) {
        List<Map<String, Object>> wards = getWards(districtId);

        return wards.stream()
                .filter(w -> {
                    String name = (String) w.get("WardName");
                    return name != null && name.equalsIgnoreCase(wardName.trim());
                })
                .map(w -> (String) w.get("WardCode"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ward [" + wardName + "] not found in GHN for the given district. " +
                                "Please use exact GHN ward name."));
    }

    public GhnAddressResult validateFullAddressById(String province,
                                                    Integer districtId,
                                                    String wardCode) {
        log.info("Validating GHN address: province={}, districtId={}, wardCode={}",
                province, districtId, wardCode);

        // Step 1: Validate province name → get province ID
        Integer provinceId = validateAndGetProvinceId(province);

        // Step 2: Validate districtId exists under this province
        List<Map<String, Object>> districts = getDistricts(provinceId);
        Map<String, Object> matchedDistrict = districts.stream()
                .filter(d -> districtId.equals(d.get("DistrictID")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "District ID [" + districtId + "] not found under province [" + province + "]. " +
                                "Use GET /api/address/districts?provinceId=X to get valid district IDs."));

        String districtName = (String) matchedDistrict.get("DistrictName");

        // Step 3: Validate wardCode exists under this district
        List<Map<String, Object>> wards = getWards(districtId);
        Map<String, Object> matchedWard = wards.stream()
                .filter(w -> wardCode.equals(w.get("WardCode")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ward code [" + wardCode + "] not found under district ID [" + districtId + "]. " +
                                "Use GET /api/address/wards?districtId=X to get valid ward codes."));

        String wardName = (String) matchedWard.get("WardName");

        log.info("GHN address validated: provinceId={}, districtId={}, wardCode={}",
                provinceId, districtId, wardCode);

        return GhnAddressResult.builder()
                .provinceId(provinceId)
                .districtId(districtId)
                .wardCode(wardCode)
                .provinceName(province)
                .districtName(districtName)
                .wardName(wardName)
                .build();
    }

    // ── VALIDATE PROVINCE NAME → RETURN PROVINCE ID ───────────────────────
    public Integer validateAndGetProvinceId(String provinceName) {
        List<Map<String, Object>> provinces = getProvinces();

        return provinces.stream()
                .filter(p -> {
                    String name = (String) p.get("ProvinceName");
                    return name != null && name.equalsIgnoreCase(provinceName.trim());
                })
                .map(p -> (Integer) p.get("ProvinceID"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Province [" + provinceName + "] not found in GHN. " +
                                "Please use exact GHN province name."));
    }

    // ── VALIDATE FULL ADDRESS AT ONCE ─────────────────────────────────────
    public GhnAddressResult validateFullAddress(String province, String district, String ward) {
        log.info("Validating GHN address: province={}, district={}, ward={}", province, district, ward);

        Integer provinceId  = validateAndGetProvinceId(province);
        Integer districtId  = validateAndGetDistrictId(provinceId, district);
        String wardCode     = validateAndGetWardCode(districtId, ward);

        log.info("GHN address validated: provinceId={}, districtId={}, wardCode={}",
                provinceId, districtId, wardCode);

        return GhnAddressResult.builder()
                .provinceId(provinceId)
                .districtId(districtId)
                .wardCode(wardCode)
                .provinceName(province)
                .districtName(district)
                .wardName(ward)
                .build();
    }

    @Data
    @Builder
    public static class GhnAddressResult {
        private Integer provinceId;
        private Integer districtId;
        private String wardCode;
        private String provinceName;
        private String districtName;
        private String wardName;
    }
}
