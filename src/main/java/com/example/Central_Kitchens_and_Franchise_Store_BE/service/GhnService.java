package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.GhnConfig;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.CentralFoods;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnCreateOrderPayload;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.GhnMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodsRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.RandomGeneratorUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final RestTemplate restTemplate;
    private final GhnConfig ghnConfig;
    private final ObjectMapper objectMapper;
    private final RandomGeneratorUtil randomGeneratorUtil;
    private final CentralFoodsRepository centralFoodsRepository;
    private final GhnMapper ghnMapper;

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

        List<CentralFoods> centralFoods = validateAndFetchFoods(request.getFoods());

        List<GhnItem> items =
                ghnMapper.convertToGhnItems(request.getFoods(), centralFoods);

        GhnCreateOrderPayload payload =
                buildGhnPayload(request, items);

        return callGhnCreateOrderApi(payload);
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
    public Map<String, Object> calculateFeeFromOrder(String orderCode) {
        // Step 1: Track the order to get all needed fields
        Map<String, Object> trackResponse = trackOrder(orderCode);

        if (trackResponse == null || !Integer.valueOf(200).equals(trackResponse.get("code"))) {
            throw new RuntimeException("Failed to fetch order info for code: " + orderCode);
        }

        Map<String, Object> orderData = (Map<String, Object>) trackResponse.get("data");

        // Step 2: Extract fields from tracked order
        Integer fromDistrictId = (Integer) orderData.get("from_district_id");
        String fromWardCode    = (String)  orderData.get("from_ward_code");
        Integer toDistrictId   = (Integer) orderData.get("to_district_id");
        String toWardCode      = (String)  orderData.get("to_ward_code");
        Integer weight         = (Integer) orderData.get("weight");
        Integer serviceTypeId  = (Integer) orderData.get("service_type_id");

        log.info("Extracted from tracked order [{}]: fromDistrict={}, toDistrict={}, weight={}, serviceType={}",
                orderCode, fromDistrictId, toDistrictId, weight, serviceTypeId);

        // Step 3: Build fee request body using extracted fields
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/fee";

        Map<String, Object> body = Map.of(
                "from_district_id", fromDistrictId,
                "from_ward_code",   fromWardCode,
                "to_district_id",   toDistrictId,
                "to_ward_code",     toWardCode,
                "weight",           weight,
                "service_type_id",  serviceTypeId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(true));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            log.info("GHN Calculate Fee Response: {}", response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("GHN Calculate Fee Error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("GHN API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    private String generateDeliverOrderId(Integer paymentTypeId, Integer serviceTypeId) {
        String paymentPrefix = switch (paymentTypeId) {
            case 1 -> "SE"; // Sender pays
            case 2 -> "RE"; // Receiver pays
            default -> throw new IllegalArgumentException("Invalid payment_type_id");
        };

        String servicePrefix = switch (serviceTypeId) {
            case 1 -> "EX"; // Express
            case 2 -> "ST"; // Standard
            default -> throw new IllegalArgumentException("Invalid service_type_id");
        };

        return "DO_" + paymentPrefix + "_" + servicePrefix + "_" + randomGeneratorUtil.randomSix();

    }
//----------------------------SUPPORTING FUNCTION----------------------------------------------------------------------
    private List<CentralFoods> validateAndFetchFoods(Map<String, Integer> foods) {

        List<String> foodIds = new ArrayList<>(foods.keySet());

        List<CentralFoods> centralFoods =
                centralFoodsRepository.findByCentralFoodIdIn(foodIds);

        Set<String> foundIds = centralFoods.stream()
                .map(CentralFoods::getCentralFoodId)
                .collect(Collectors.toSet());

        List<String> missingIds = foodIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        List<String> invalidStatusFoods = centralFoods.stream()
                .filter(food -> food.getCentralFoodStatus() != FoodStatus.AVAILABLE)
                .map(CentralFoods::getCentralFoodId)
                .toList();

        if (!invalidStatusFoods.isEmpty()) {
            throw new IllegalStateException(
                    "Food not available: " + invalidStatusFoods);
        }


        return centralFoods;
    }

    private GhnCreateOrderPayload buildGhnPayload(
            CreateDeliveryOrderRequest request,
            List<GhnItem> items) {

        return GhnCreateOrderPayload.builder()
                .payment_type_id(request.getPayment_type_id())
                .note(request.getNote())
                .required_note(request.getRequired_note())
                .to_name(request.getTo_name())
                .to_phone(request.getTo_phone())
                .to_address(request.getTo_address())
                .to_ward_code(request.getTo_ward_code())
                .to_district_id(request.getTo_district_id())
                .cod_amount(request.getCod_amount())
                .weight(request.getWeight())
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .service_type_id(request.getService_type_id())
                .items(items)
                .client_order_code(generateDeliverOrderId(
                        request.getPayment_type_id(),
                        request.getService_type_id()
                ))
                .build();
    }

    private Map<String, Object> callGhnCreateOrderApi(
            GhnCreateOrderPayload payload) {

        String url = ghnConfig.getBaseUrl()
                + "/shiip/public-api/v2/shipping-order/create";

        HttpEntity<GhnCreateOrderPayload> entity =
                new HttpEntity<>(payload, buildHeaders(true));

        try {

            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Map.class
                    );

            log.info("GHN Status: {}", response.getStatusCode());
            log.info("GHN Response: {}", response.getBody());

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {

            log.error("GHN API ERROR STATUS: {}", e.getStatusCode());
            log.error("GHN API ERROR BODY: {}", e.getResponseBodyAsString());

            throw new RuntimeException(
                    "GHN API error: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

        } catch (Exception e) {

            log.error("Unexpected error calling GHN", e);
            throw new RuntimeException(
                    "Unexpected GHN error: " + e.getMessage());
        }
    }
}
