package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.config.GhnConfig;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ShipInvoiceResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateDeliveryOrderRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.InvalidOperationException;
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
import org.springframework.scheduling.annotation.Scheduled;
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
    private final FranchiseStoreRepository franchiseStoreRepository;


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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OrderDetail not found: " + request.getOrderDetailId()));

        // ── Check PaymentOption: PAY_AFTER_ORDER phải thanh toán trước khi tạo đơn giao
        Order order = orderRepository.findById(orderDetail.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found for orderDetail: " + request.getOrderDetailId()));

        if (PaymentOption.PAY_AFTER_ORDER.equals(order.getPaymentOption())
                && order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Order [" + order.getOrderId() + "] sử dụng PAY_AFTER_ORDER " +
                            "nhưng chưa thanh toán (current status: " + order.getPaymentStatus() + "). " +
                            "Vui lòng thanh toán trước khi tạo đơn giao hàng."
            );
        }


        // ── Check Order phải ở trạng thái COOKING_DONE trước khi tạo đơn giao
        if (order.getStatusOrder() != OrderStatus.COOKING_DONE) {
            throw new IllegalStateException(
                    "Order [" + order.getOrderId() + "] chưa hoàn tất nấu " +
                            "(current status: " + order.getStatusOrder() + "). " +
                            "Vui lòng chờ đến khi trạng thái là COOKING_DONE."
            );
        }

        // Step 2: Find FranchiseStore — delivery destination
        FranchiseStore store = franchiseStoreRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found: " + request.getStoreId()));

        if (orderDetail.getShipment() != null) {
            throw new InvalidOperationException(
                    "OrderDetail [" + request.getOrderDetailId() + "] " +
                            "already has a shipment [" + orderDetail.getShipment().getShipmentCodeId() + "]. " +
                            "Cannot create duplicate delivery order.");
        }

        if (store.getDistrict() == null || store.getWard() == null) {
            throw new IllegalStateException(
                    "Store [" + request.getStoreId() + "] has no valid GHN address. " +
                            "Please update the store address first.");
        }


        Order linkedOrder = orderDetail.getOrder();
        if (linkedOrder == null) {
            throw new ResourceNotFoundException(
                    "OrderDetail [" + request.getOrderDetailId() + "] has no linked Order.");
        }

        if (!request.getStoreId().equals(order.getStoreId())) {
            throw new IllegalArgumentException(
                    "OrderDetail [" + request.getOrderDetailId() + "] " +
                            "belongs to store [" + order.getStoreId() + "], " +
                            "not store [" + request.getStoreId() + "]. " +
                            "Cannot create delivery for a different store.");
        }

        Integer codAmount = 0;
        if (PaymentOption.PAY_AFTER_DELIVERY.equals(order.getPaymentOption())) {
            codAmount = orderDetail.getAmount() != null
                    ? orderDetail.getAmount().intValue()
                    : 0;
            log.info("PAY_AFTER_DELIVERY — COD amount set to: {}", codAmount);
        } else {
            log.info("PaymentOption={} — COD amount set to 0", order.getPaymentOption());
        }

        // Step 3: Validate foods
        Map<String, Integer> foods = extractFoodsFromOrderDetail(orderDetail);
        List<CentralFoods> centralFoods = validateAndFetchFoods(foods);

        // Step 4: Auto-calculate total weight and max dimensions from food items
        PackageDimensions dimensions = calculateDimensions(foods, centralFoods);
        log.info("Calculated package: weight={}g, {}x{}x{}cm",
                dimensions.weight(), dimensions.length(), dimensions.width(), dimensions.height());

        // Step 5: Convert to GHN items
        List<GhnItem> items = ghnMapper.convertToGhnItems(foods, centralFoods);

        // Step 6: Generate client order code
        String clientOrderCode = generateDeliverOrderId(
                request.getPayment_type_id(),
                2
        );
        // Step 7: Call GHN API
        Map<String, Object> ghnResponse = callGhnCreateOrderApi(request, items, clientOrderCode, store, dimensions, codAmount);

        // Step 8: Extract GHN response data
        Map<String, Object> data = (Map<String, Object>) ghnResponse.get("data");
        String ghnOrderCode = (String) data.get("order_code");
        String ghnStatus    = (String) data.get("status");
        ShipmentStatus newShipmentStatus = ghnStatusMapper.toShipmentStatus(ghnStatus);

        // Step 9: Save Shipment
        Shipment shipment = buildShipmentPayload(request, items, store, dimensions, clientOrderCode, codAmount);
        shipment.setShipmentCodeId(clientOrderCode);
        shipment.setGhnOrderCode(ghnOrderCode);
        shipment.setOrderDetailId(orderDetail.getOrderDetailId());
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setUpdatedAt(LocalDateTime.now());
        shipment.setShipStatus(newShipmentStatus);

        Shipment savedShipment = shipmentRepository.save(shipment);

        // Step 10: Save ShipInvoice
        ShipPaymentType paymentType = switch (request.getPayment_type_id()) {
            case 1 -> ShipPaymentType.SENDER_PAY;
            case 2 -> ShipPaymentType.RECEIVER_PAY;
            default -> throw new RuntimeException("Invalid payment_type_id");
        };

        ShipInvoice shipInvoice = ShipInvoice.builder()
                .shipInvoiceId(generateShipInvoiceId())
                .shipmentCodeId(savedShipment.getShipmentCodeId())
                .paymentType(paymentType)
                .totalPrice(BigDecimal.ZERO)
                .invoiceStatus(InvoiceStatus.PENDING)
                .build();

        shipInvoiceRepository.save(shipInvoice);
        log.info("Order created — GHN: {}, ShipInvoice: {}",
                ghnOrderCode, shipInvoice.getShipInvoiceId());

        calculateFeeFromOrder(ghnOrderCode);

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
    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<ShipmentInfo> getAllShipmentsByStoreId(String storeId,
                                                       ShipmentStatus status) {
        log.info("Fetching shipments for storeId: {}, status: {}", storeId, status);

        // Validate store exists
        franchiseStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found: " + storeId));

        List<Shipment> shipments = (status != null)
                ? shipmentRepository.findAllByStoreIdAndStatus(storeId, status)
                : shipmentRepository.findAllByStoreId(storeId);

        log.info("Found {} shipments for store [{}]", shipments.size(), storeId);

        return ghnMapper.convertToDTOList(shipments);
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

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncShipmentStatuses() {
        // Only sync active shipments — no point checking delivered/cancelled
        List<ShipmentStatus> activeStatuses = List.of(
                ShipmentStatus.READY_TO_PICK,
                ShipmentStatus.PICKING,
                ShipmentStatus.PICKED,
                ShipmentStatus.DELIVERING,
                ShipmentStatus.DELIVERY_FAILED,
                ShipmentStatus.WAITING_TO_RETURN
        );

        List<Shipment> activeShipments = shipmentRepository
                .findAllByShipStatusIn(activeStatuses);

        if (activeShipments.isEmpty()) {
            log.debug("No active shipments to sync");
            return;
        }

        log.info("Syncing {} active shipments...", activeShipments.size());

        int updated = 0;
        int failed  = 0;

        for (Shipment shipment : activeShipments) {
            try {
                boolean wasUpdated = syncSingleShipment(shipment);
                if (wasUpdated) updated++;

            } catch (Exception e) {
                failed++;
                log.error("Failed to sync shipment [{}]: {}",
                        shipment.getShipmentCodeId(), e.getMessage());
            }
        }

        log.info("Sync complete — updated: {}, failed: {}, unchanged: {}",
                updated, failed, activeShipments.size() - updated - failed);
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
            shipInvoice.setInvoiceStatus(InvoiceStatus.PENDING);
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

    private boolean syncSingleShipment(Shipment shipment) {
        // Step 1: Get live status from GHN
        Map<String, Object> trackResponse = trackOrder(
                shipment.getGhnOrderCode());

        if (trackResponse == null ||
                !Integer.valueOf(200).equals(trackResponse.get("code"))) {
            log.warn("GHN returned non-200 for shipment [{}]",
                    shipment.getShipmentCodeId());
            return false;
        }

        Map<String, Object> data = (Map<String, Object>) trackResponse.get("data");
        String ghnStatus = (String) data.get("status");

        if (ghnStatus == null) {
            log.warn("GHN returned null status for shipment [{}]",
                    shipment.getShipmentCodeId());
            return false;
        }

        // Step 2: Map to your enum
        ShipmentStatus newShipmentStatus = ghnStatusMapper.toShipmentStatus(ghnStatus);
        OrderStatus newOrderStatus       = ghnStatusMapper.toOrderStatus(ghnStatus);

        // Step 3: Compare with DB — only update if different
        if (newShipmentStatus.equals(shipment.getShipStatus())) {
            log.debug("Shipment [{}] status unchanged: {}",
                    shipment.getShipmentCodeId(), newShipmentStatus);
            return false;
        }

        log.info("Shipment [{}] status changed: {} → {} (GHN: {})",
                shipment.getShipmentCodeId(),
                shipment.getShipStatus(),
                newShipmentStatus,
                ghnStatus);

        // Step 4: Update Shipment
        shipment.setShipStatus(newShipmentStatus);
        shipment.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // Step 5: Update linked Order status
        if (shipment.getOrderDetailId() != null) {
            orderDetailRepository.findByOrderDetailId(shipment.getOrderDetailId())
                    .ifPresent(orderDetail -> {
                        Order order = orderDetail.getOrder();
                        if (order != null) {
                            order.setStatusOrder(newOrderStatus);
                            orderRepository.save(order);
                            log.info("Order [{}] statusOrder → {}",
                                    order.getOrderId(), newOrderStatus);
                        }
                    });
        }

        return true;
    }

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

    private Shipment buildShipmentPayload(CreateDeliveryOrderRequest request,
                                          List<GhnItem> items,
                                          FranchiseStore store,
                                          PackageDimensions dim,
                                          String clientOrderCode,
                                          Integer codAmount) {

        return Shipment.builder()
                .note(request.getNote())
                .required_note(RequiredNote.CHOXEMHANGKHONGTHU)
                .to_name(request.getTo_name())
                .to_phone(request.getTo_phone())
                .to_address(store.getAddress())        // ← from store
                .to_ward_code(store.getWard())             // ← from store
                .to_district_id(store.getDistrict())       // ← from store
                .cod_amount(codAmount)
                .weight(dim.weight())                      // ← calculated
                .length(dim.length())                      // ← calculated
                .width(dim.width())                        // ← calculated
                .height(dim.height())                      // ← calculated
                .service_type(ShipServiceType.STANDARD)
                .items(items)
                .client_order_code(clientOrderCode)
                .build();
    }

    private Map<String, Object> callGhnCreateOrderApi(CreateDeliveryOrderRequest request,
                                                      List<GhnItem> items,
                                                      String clientOrderCode,
                                                      FranchiseStore store,
                                                      PackageDimensions dim,
                                                      Integer codAmount) {
        String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/create";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_type_id",   request.getPayment_type_id());
        requestBody.put("note",              request.getNote());
        requestBody.put("required_note",     RequiredNote.CHOXEMHANGKHONGTHU.name());
        requestBody.put("to_name",           request.getTo_name());
        requestBody.put("to_phone",          request.getTo_phone());
        requestBody.put("to_address",        store.getAddress());
        requestBody.put("to_ward_code",      store.getWard());          // ← store
        requestBody.put("to_district_id",    store.getDistrict());      // ← store
        requestBody.put("cod_amount",        codAmount);
        requestBody.put("weight",            dim.weight());             // ← calculated
        requestBody.put("length",            dim.length());             // ← calculated
        requestBody.put("width",             dim.width());              // ← calculated
        requestBody.put("height",            dim.height());             // ← calculated
        requestBody.put("service_type_id",   (Integer) 2);
        requestBody.put("items",             items);
        requestBody.put("client_order_code", clientOrderCode);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders(true));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);
            log.info("GHN Status: {}, Response: {}", response.getStatusCode(), response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("GHN API ERROR: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("GHN API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Unexpected GHN error", e);
            throw new RuntimeException("Unexpected GHN error: " + e.getMessage());
        }
    }

    public List<ShipInvoiceResponse> getAllInvoices(InvoiceStatus status) {
        List<ShipInvoice> invoices = (status != null)
                ? shipInvoiceRepository.findByInvoiceStatus(status)
                : shipInvoiceRepository.findAll();

        return invoices.stream()
                .map(ghnMapper::toShipInvoiceResponse)
                .toList();
    }

    public ShipInvoiceResponse getInvoiceById(String shipInvoiceId) {
        ShipInvoice invoice = shipInvoiceRepository.findById(shipInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + shipInvoiceId));
        return ghnMapper.toShipInvoiceResponse(invoice);
    }

    private record PackageDimensions(int weight, int length, int width, int height) {}

    private PackageDimensions calculateDimensions(Map<String, Integer> foods,
                                                  List<CentralFoods> centralFoods) {
        Map<String, CentralFoods> foodMap = centralFoods.stream()
                .collect(Collectors.toMap(CentralFoods::getCentralFoodId, f -> f));

        int totalWeight = 0;
        int maxLength   = 0;
        int maxWidth    = 0;
        int maxHeight   = 0;

        for (Map.Entry<String, Integer> entry : foods.entrySet()) {
            String foodId  = entry.getKey();
            int quantity   = entry.getValue();
            CentralFoods food = foodMap.get(foodId);

            if (food.getWeight() == null || food.getLength() == null
                    || food.getWidth() == null || food.getHeight() == null) {
                throw new IllegalStateException(
                        "Food [" + foodId + "] is missing dimension data (weight/length/width/height). " +
                                "Please update the food record.");
            }

            totalWeight += food.getWeight() * quantity;
            maxLength    = Math.max(maxLength, food.getLength());
            maxWidth     = Math.max(maxWidth,  food.getWidth());
            maxHeight   += food.getHeight() * quantity; // stack height for multiple items
        }

        return new PackageDimensions(totalWeight, maxLength, maxWidth, maxHeight);
    }

    private Map<String, Integer> extractFoodsFromOrderDetail(OrderDetail orderDetail) {
        if (orderDetail.getOrderDetailItems() == null ||
                orderDetail.getOrderDetailItems().isEmpty()) {
            throw new IllegalStateException(
                    "OrderDetail [" + orderDetail.getOrderDetailId() +
                            "] has no items. Cannot create delivery order.");
        }

        // Build Map<centralFoodId, quantity> from order detail items
        Map<String, Integer> foods = orderDetail.getOrderDetailItems().stream()
                .collect(Collectors.toMap(
                        OrderDetailItem::getCentralFoodId,
                        OrderDetailItem::getQuantity,
                        Integer::sum  // merge if same food appears twice
                ));

        log.info("Extracted {} food items from OrderDetail [{}]: {}",
                foods.size(), orderDetail.getOrderDetailId(), foods);

        return foods;
    }
}