package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.GhnConfig;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.GhnItem;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipmentInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.integration.ghn.ShipmentStatusUpdateResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.GhnMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.GhnStatusMapper;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.RandomGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final GhnConfig ghnConfig;
    private final ShipmentRepository shipmentRepository;
    private final ShipInvoiceRepository shipInvoiceRepository;
    private final RandomGeneratorUtil randomGeneratorUtil;
    private final CentralFoodsRepository centralFoodsRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final GhnMapper ghnMapper;
    private final GhnStatusMapper ghnStatusMapper;


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

    @Transactional
    public Map<String, Object> createOrder(CreateDeliveryOrderRequest request) {

        log.info("Creating order with payment_type_id: {}", request.getPayment_type_id());

        OrderDetail orderDetail = orderDetailRepository.findByOrderDetailId(request.getOrderDetailId())
                .orElseThrow(() -> new ResourceNotFoundException("Order details with this id " + request.getOrderDetailId() + " does not existed!!!"));

        // Step 1: Validate foods
        List<CentralFoods> centralFoods = validateAndFetchFoods(request.getFoods());

        // Step 2: Convert to GHN items
        List<GhnItem> items = ghnMapper.convertToGhnItems(request.getFoods(), centralFoods);

        // Step 3: Build Shipment payload
        Shipment shipment = buildShipmentPayload(request, items);

        // Step 4: Call GHN API
        Map<String, Object> ghnResponse = callGhnCreateOrderApi(request, items, shipment.getGhnOrderCode());

        // Step 5: Extract GHN order code from response
        Map<String, Object> data = (Map<String, Object>) ghnResponse.get("data");
        String ghnOrderCode = (String) data.get("order_code");
        String ghnStatus = (String) data.get("status");
        ShipmentStatus newShipmentStatus = ghnStatusMapper.toShipmentStatus(ghnStatus);


        // Step 6: Save Shipment to database
        shipment.setShipmentCodeId(shipment.getClient_order_code()); // Use client_order_code as ID
        shipment.setGhnOrderCode(ghnOrderCode);
        shipment.setOrderDetailId(orderDetail.getOrderDetailId()); // set order detail
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setUpdatedAt(LocalDateTime.now());
        shipment.setShipStatus(newShipmentStatus);

        Shipment savedShipment = shipmentRepository.save(shipment);

        ShipPaymentType paymentType = null;
        if(request.getPayment_type_id() == 1) {
            paymentType = ShipPaymentType.SENDER_PAY;
        } else if(request.getPayment_type_id() == 2) {
            paymentType = ShipPaymentType.RECEIVER_PAY;
        } else {
            throw new RuntimeException("Payment id is null or is not 1, 2!!!!");
        }

        // Step 7: Create and save ShipInvoice
        ShipInvoice shipInvoice = ShipInvoice.builder()
                .shipInvoiceId(generateShipInvoiceId())
                .shipmentCodeId(savedShipment.getShipmentCodeId())
                .paymentType(paymentType)
                .totalPrice(BigDecimal.ZERO)// Will be updated when fee is calculated
                .build();

        shipInvoiceRepository.save(shipInvoice);

        log.info("ShipInvoice created with ID: {}", shipInvoice.getShipInvoiceId());

        return ghnResponse;
    }


    //---GET ALL-------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ShipmentInfo> getAllShipments() {
        log.info("Fetching all shipments with ship invoices");

        // Fetch all shipments with ship invoices eagerly loaded
        List<Shipment> shipments = shipmentRepository.findAll();

        log.info("Found {} shipments", shipments.size());

        return ghnMapper.convertToDTOList(shipments);
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
    //------------------------------------------------------------------------------------------------------------------------
    @Transactional
    public ShipmentStatusUpdateResponse updateShipmentStatus(String shipmentId) {
        log.info("Updating shipment status for shipment ID: {}", shipmentId);

        // Step 1: Find Shipment


        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found with ID: " + shipmentId));

        OrderDetail orderDetail = orderDetailRepository.findByOrderDetailId(shipment.getOrderDetailId())
                .orElseThrow(() -> new ResourceNotFoundException("Order details with this id " + shipment.getOrderDetailId() + " does not existed!!!"));

        // Step 2: Call GHN to get latest status
        String ghnOrderCode = shipment.getGhnOrderCode();
        Map<String, Object> trackResponse = trackOrder(ghnOrderCode);

        if (trackResponse == null || !Integer.valueOf(200).equals(trackResponse.get("code"))) {
            throw new RuntimeException("Failed to fetch GHN status for: " + ghnOrderCode);
        }

        Map<String, Object> data = (Map<String, Object>) trackResponse.get("data");
        String ghnStatus = (String) data.get("status");
        log.info("GHN status for order [{}]: {}", ghnOrderCode, ghnStatus);

        // Step 3: Map GHN status → your enums
        ShipmentStatus newShipmentStatus = ghnStatusMapper.toShipmentStatus(ghnStatus);
        OrderStatus newOrderStatus       = ghnStatusMapper.toOrderStatus(ghnStatus);

        // Step 4: Update Shipment
        shipment.setShipStatus(newShipmentStatus);
        shipment.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);
        log.info("Shipment [{}] → {}", shipmentId, newShipmentStatus);

        // Step 5: Navigate Shipment → OrderDetail → Order and update statusOrder


        Order order = orderDetail.getOrder();
        if (order == null) {
            throw new ResourceNotFoundException(
                    "No Order linked to OrderDetail: " + orderDetail.getOrderDetailId());
        }

        order.setStatusOrder(newOrderStatus);   // ← uses your existing statusOrder field
        orderRepository.save(order);
        log.info("Order [{}] statusOrder → {}", order.getOrderId(), newOrderStatus);

        // Step 6: Return result
        return ShipmentStatusUpdateResponse.builder()
                .shipmentId(shipmentId)
                .ghnOrderCode(ghnOrderCode)
                .ghnRawStatus(ghnStatus)
                .shipmentStatus(newShipmentStatus)
                .orderDetailId(orderDetail.getOrderDetailId())
                .orderId(order.getOrderId())
                .orderStatus(newOrderStatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── CALCULATE FEE ────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> calculateFeeFromOrder(String orderCode) {
        log.info("Calculating fee for order: {}", orderCode);

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

            Map<String, Object> feeResponse = response.getBody();

            // Step 4: Extract fee from response
            Map<String, Object> feeData = (Map<String, Object>) feeResponse.get("data");
            Integer totalFee = (Integer) feeData.get("total");

            log.info("Total fee calculated: {}", totalFee);

            // Step 5: Find Shipment by GHN order code
            Shipment shipment = shipmentRepository.findByGhnOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Shipment not found for GHN order code: " + orderCode));

            // Step 6: Update ShipInvoice with total price
            ShipInvoice shipInvoice = shipInvoiceRepository.findByShipmentCodeId(shipment.getShipmentCodeId())
                    .orElseThrow(() -> new RuntimeException("ShipInvoice not found for shipment: " + shipment.getShipmentCodeId()));

            shipInvoice.setTotalPrice(BigDecimal.valueOf(totalFee));
            shipInvoiceRepository.save(shipInvoice);


            return feeResponse;

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

    private String generateShipInvoiceId() {
        return "SI_" + randomGeneratorUtil.randomSix();
    }

    //----------------------------SUPPORTING FUNCTION----------------------------------------------------------------------

    private List<CentralFoods> validateAndFetchFoods(Map<String, Integer> foods) {

        List<String> foodIds = new ArrayList<>(foods.keySet());

        List<CentralFoods> centralFoods = centralFoodsRepository.findByCentralFoodIdIn(foodIds);

        Set<String> foundIds = centralFoods.stream()
                .map(CentralFoods::getCentralFoodId)
                .collect(Collectors.toSet());

        List<String> missingIds = foodIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Foods not found: " + missingIds);
        }

        List<String> invalidStatusFoods = centralFoods.stream()
                .filter(food -> food.getCentralFoodStatus() != FoodStatus.AVAILABLE)
                .map(CentralFoods::getCentralFoodId)
                .toList();

        if (!invalidStatusFoods.isEmpty()) {
            throw new IllegalStateException("Food not available: " + invalidStatusFoods);
        }

        return centralFoods;
    }

    private Shipment buildShipmentPayload(CreateDeliveryOrderRequest request, List<GhnItem> items) {


        ShipServiceType serviceType = null;
        if(request.getService_type_id() == 1) {
            serviceType = ShipServiceType.EXPRESS;
        } else if(request.getService_type_id() == 2) {
            serviceType = ShipServiceType.STANDARD;
        } else {
            throw new RuntimeException("Service type id is null or is not 1, 2!!!!!");
        }

        return Shipment.builder()
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
                .service_type(serviceType)
                .items(items) // Transient field for GHN API
                .client_order_code(generateDeliverOrderId(
                        request.getPayment_type_id(),
                        request.getService_type_id()
                ))
                .build();
    }

    private Map<String, Object> callGhnCreateOrderApi(CreateDeliveryOrderRequest createDeliveryOrderRequest,
                                                      List<GhnItem> items, String clientOrderCode) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/create";

        // Build request body for GHN API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_type_id", createDeliveryOrderRequest.getPayment_type_id());
        requestBody.put("note", createDeliveryOrderRequest.getNote());
        requestBody.put("required_note", createDeliveryOrderRequest.getRequired_note());
        requestBody.put("to_name", createDeliveryOrderRequest.getTo_name());
        requestBody.put("to_phone", createDeliveryOrderRequest.getTo_phone());
        requestBody.put("to_address", createDeliveryOrderRequest.getTo_address());
        requestBody.put("to_ward_code", createDeliveryOrderRequest.getTo_ward_code());
        requestBody.put("to_district_id", createDeliveryOrderRequest.getTo_district_id());
        requestBody.put("cod_amount", createDeliveryOrderRequest.getCod_amount());
        requestBody.put("weight", createDeliveryOrderRequest.getWeight());
        requestBody.put("length", createDeliveryOrderRequest.getLength());
        requestBody.put("width", createDeliveryOrderRequest.getWidth());
        requestBody.put("height", createDeliveryOrderRequest.getHeight());
        requestBody.put("service_type_id", createDeliveryOrderRequest.getService_type_id());
        requestBody.put("items", items);
        requestBody.put("client_order_code", clientOrderCode);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders(true));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
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
                    "GHN API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            log.error("Unexpected error calling GHN", e);
            throw new RuntimeException("Unexpected GHN error: " + e.getMessage());
        }
    }
}