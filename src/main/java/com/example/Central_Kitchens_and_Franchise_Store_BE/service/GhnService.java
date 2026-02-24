package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.GhnConfig;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final RestTemplate restTemplate;
    private final GhnConfig ghnConfig;
    private final ObjectMapper objectMapper;

    private HttpHeaders buildHeaders(boolean includeShopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        if (includeShopId) {
            headers.set("ShopId", ghnConfig.getShopId());
        }
        return headers;
    }

    // ─── CREATE ORDER ─────────────────────────────────────────────────────────
    public Map<String, Object> createOrder(CreateDeliveryOrderRequest request) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/create";

        HttpEntity<CreateDeliveryOrderRequest> entity = new HttpEntity<>(request, buildHeaders(true));

        try {

            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Map.class
                    );

            log.info("GHN Status: {}", response.getStatusCode());
            log.info("GHN Response Body: {}", response.getBody());

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {

            log.error("GHN API ERROR STATUS: {}", e.getStatusCode());
            log.error("GHN API ERROR BODY: {}", e.getResponseBodyAsString());

            throw new RuntimeException(
                    "GHN API error: " +
                            e.getStatusCode() +
                            " - " +
                            e.getResponseBodyAsString()
            );

        } catch (Exception e) {

            log.error("Unexpected error calling GHN", e);
            throw new RuntimeException("Unexpected GHN error: " + e.getMessage());
        }
    }

    // ─── TRACK ORDER ──────────────────────────────────────────────────────────
    public Map<String, Object> trackOrder(String orderCode) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/detail";

        Map<String, String> body = Map.of("order_code", orderCode);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, buildHeaders(false));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            log.info("GHN Track Order Response: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GHN Track Order Error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("GHN API error: " + e.getResponseBodyAsString());
        }
    }

    // ─── CALCULATE FEE ────────────────────────────────────────────────────────
    public Map<String, Object> calculateFee(int fromDistrictId, String fromWardCode,
                                            int toDistrictId, String toWardCode,
                                            int weight, int serviceTypeId) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/fee";

        Map<String, Object> body = Map.of(
                "from_district_id", fromDistrictId,
                "from_ward_code", fromWardCode,
                "to_district_id", toDistrictId,
                "to_ward_code", toWardCode,
                "weight", weight,
                "service_type_id", serviceTypeId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(true));
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return response.getBody();
    }
}
