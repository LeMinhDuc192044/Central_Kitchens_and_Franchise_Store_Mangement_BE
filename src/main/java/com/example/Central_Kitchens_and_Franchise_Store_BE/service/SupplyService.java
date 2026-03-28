package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AggregatePreviewResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.SupplyBatchItemResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.SupplyBatchResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateBatchRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdateBatchItemRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.OrderStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodsRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.KitchenConfigRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.SupplyBatchRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyService {

    private final OrderRepository        orderRepository;
    private final SupplyBatchRepository  supplyBatchRepository;
    private final CentralFoodsRepository centralFoodsRepository;
    private final KitchenConfigRepository kitchenConfigRepository;

    private int getMaxTypesPerDay() {
        return kitchenConfigRepository.findByConfigKey("MAX_TYPES_PER_DAY")
                .map(KitchenConfig::getIntValue)
                .orElse(10);
    }

    private int getMaxQuantityPerDay() {
        return kitchenConfigRepository.findByConfigKey("MAX_QUANTITY_PER_DAY")
                .map(KitchenConfig::getIntValue)
                .orElse(40);
    }

    // ═══════════════════════════════════════════════════════════════
// 1. PREVIEW - Xem trước tổng hợp cuối ngày mà CHƯA tạo batch
//    Supply coordinator dùng để kiểm tra trước khi quyết định gửi
// ═══════════════════════════════════════════════════════════════
    @Transactional
    public AggregatePreviewResponse previewAggregation(LocalDate date) {
        List<Order> orders = orderRepository.findByStatusOrderAndOrderDate(
                OrderStatus.WAITING_FOR_PRODUCTION, date);

        if (orders.isEmpty()) {
            return AggregatePreviewResponse.builder()
                    .totalTypes(0)
                    .totalQuantity(0)
                    .estimatedBatchCount(0)
                    .warning("Không có đơn WAITING_FOR_PRODUCTION nào được tạo trong ngày " + date)
                    .aggregatedItems(Collections.emptyList())
                    .build();
        }

        // Gom món: foodId → {totalQty, sourceDetail}
        Map<String, AggregatedFoodData> foodMap = aggregateFoods(orders);

        int totalTypes    = foodMap.size();
        int totalQuantity = foodMap.values().stream().mapToInt(f -> f.totalQty).sum();

        // Build aggregated items response
        List<AggregatePreviewResponse.AggregatedFoodItem> aggregatedItems = foodMap.values().stream()
                .map(f -> AggregatePreviewResponse.AggregatedFoodItem.builder()
                        .centralFoodId(f.centralFoodId)
                        .foodName(f.foodName)
                        .totalQuantity(f.totalQty)
                        .sourceDetail(f.buildSourceDetailString())
                        .build())
                .collect(Collectors.toList());

        // ✅ Chỉ cảnh báo, không block — để supply biết cần điều chỉnh trước khi aggregate
        String warning = buildWarning(totalTypes, totalQuantity);

        log.info("[PREVIEW] Date={} | {} orders | {} types | {} items{}",
                date, orders.size(), totalTypes, totalQuantity,
                warning != null ? " | ⚠ " + warning : "");

        return AggregatePreviewResponse.builder()
                .totalTypes(totalTypes)
                .totalQuantity(totalQuantity)
                .estimatedBatchCount(warning != null ? 0 : 1) // 0 = không thể tạo batch
                .warning(warning)
                .aggregatedItems(aggregatedItems)
                .build();
    }

    private String buildWarning(int totalTypes, int totalQuantity) {
        List<String> warnings = new ArrayList<>();

        if (totalQuantity > getMaxQuantityPerDay()) {
            warnings.add(String.format(
                    "tổng số món là %d, vượt giới hạn %d món/ngày",
                    totalQuantity, getMaxQuantityPerDay()));
        }
        if (totalTypes > getMaxTypesPerDay()) {
            warnings.add(String.format(
                    "tổng số loại món là %d, vượt giới hạn %d loại/ngày",
                    totalTypes, getMaxTypesPerDay()));
        }

        if (warnings.isEmpty()) return null;

        return "⚠ Không thể tạo batch: " + String.join(" và ", warnings)
                + ". Supply cần điều chỉnh lại các đơn trước khi tổng hợp.";
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. AGGREGATE - Tổng hợp cuối ngày và tạo batch thực sự
    //    Sau khi preview xong, supply xác nhận → gọi API này
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> aggregateDailyOrders(LocalDate date) {
        List<Order> orders = orderRepository.findByStatusOrderAndOrderDate(
                OrderStatus.WAITING_FOR_PRODUCTION, date);

        if (orders.isEmpty()) {
            throw new IllegalStateException(
                    "Không có đơn WAITING_FOR_PRODUCTION nào trong ngày " + date);
        }

        List<SupplyBatch> existingBatches = supplyBatchRepository.findByBatchDate(date);
        if (!existingBatches.isEmpty()) {
            throw new IllegalStateException(
                    "Đã tạo batch cho ngày " + date + ". Dùng API re-aggregate nếu muốn làm lại.");
        }

        // Gom món theo food ID
        Map<String, AggregatedFoodData> foodMap = aggregateFoods(orders);

        int totalTypes    = foodMap.size();
        int totalQuantity = foodMap.values().stream().mapToInt(f -> f.totalQty).sum();

        // ✅ Validate: vượt giới hạn → block, không tự tách batch
        if (totalQuantity > getMaxQuantityPerDay()) {
            throw new IllegalStateException(String.format(
                    "Tổng số món trong ngày %s là %d, vượt giới hạn %d món/ngày. " +
                            "Supply cần điều chỉnh lại các đơn trước khi tổng hợp.",
                    date, totalQuantity, getMaxQuantityPerDay()));
        }
        if (totalTypes > getMaxTypesPerDay()) {
            throw new IllegalStateException(String.format(
                    "Tổng số loại món trong ngày %s là %d, vượt giới hạn %d loại/ngày. " +
                            "Supply cần điều chỉnh lại các đơn trước khi tổng hợp.",
                    date, totalTypes, getMaxTypesPerDay()));
        }

        // Lúc này splitIntoBatches luôn trả về đúng 1 batch
        List<List<AggregatedFoodData>> batchGroups = splitIntoBatches(
                new ArrayList<>(foodMap.values()), orders);

        SupplyBatch batch = buildAndSaveBatch(batchGroups.get(0), date, 1, 1);

        log.info("[AGGREGATE] Date={} → Created 1 batch for {} orders", date, orders.size());

        return List.of(toResponse(batch));
    }

    // Trong SupplyService
    @Transactional
    public List<LocalDate> getAllBatchDates() {
        return supplyBatchRepository.findAllDistinctBatchDates();
    }

    // ═══════════════════════════════════════════════════════════════
    // 2b. RE-AGGREGATE - Xóa batch DRAFT cũ và tổng hợp lại
    //     Dùng khi đã aggregate rồi nhưng muốn làm lại
    //     Không cho re-aggregate nếu có batch đã SENT trở đi
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> reAggregateDailyOrders(LocalDate date) {
        List<SupplyBatch> existingBatches = supplyBatchRepository.findByBatchDate(date);

        if (!existingBatches.isEmpty()) {
            // Nếu bất kỳ batch nào đã SENT, IN_PRODUCTION, PRODUCTION_COMPLETED → không cho re-aggregate
            List<SupplyBatch> lockedBatches = existingBatches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.SENT
                            || b.getStatus() == BatchStatus.IN_PRODUCTION
                            || b.getStatus() == BatchStatus.PRODUCTION_COMPLETED)
                    .collect(Collectors.toList());

            if (!lockedBatches.isEmpty()) {
                String lockedIds = lockedBatches.stream()
                        .map(b -> b.getBatchId() + " [" + b.getStatus() + "]")
                        .collect(Collectors.joining(", "));
                throw new IllegalStateException(
                        "Không thể re-aggregate vì các batch sau đã được gửi hoặc đang sản xuất: "
                                + lockedIds);
            }

            // Chỉ có DRAFT hoặc CANCELLED → xóa hết để tạo lại
            supplyBatchRepository.deleteAll(existingBatches);
            log.info("[RE-AGGREGATE] Deleted {} old DRAFT batch(es) for date={}", existingBatches.size(), date);
        }

        // Tạo lại từ đầu
        List<Order> orders = orderRepository.findByStatusOrderAndOrderDate(
                OrderStatus.WAITING_FOR_PRODUCTION, date);

        if (orders.isEmpty()) {
            throw new IllegalStateException(
                    "Không có đơn WAITING_FOR_PRODUCTION nào trong ngày " + date);
        }

        Map<String, AggregatedFoodData> foodMap = aggregateFoods(orders);

        int totalTypes    = foodMap.size();
        int totalQuantity = foodMap.values().stream().mapToInt(f -> f.totalQty).sum();

        // ✅ Validate: vượt giới hạn → block, không tự tách batch
        if (totalQuantity > getMaxQuantityPerDay()) {
            throw new IllegalStateException(String.format(
                    "Tổng số món trong ngày %s là %d, vượt giới hạn %d món/ngày. " +
                            "Supply cần điều chỉnh lại các đơn trước khi tổng hợp.",
                    date, totalQuantity, getMaxQuantityPerDay()));
        }
        if (totalTypes > getMaxTypesPerDay()) {
            throw new IllegalStateException(String.format(
                    "Tổng số loại món trong ngày %s là %d, vượt giới hạn %d loại/ngày. " +
                            "Supply cần điều chỉnh lại các đơn trước khi tổng hợp.",
                    date, totalTypes, getMaxTypesPerDay()));
        }

        // Lúc này splitIntoBatches luôn trả về đúng 1 batch
        List<List<AggregatedFoodData>> batchGroups = splitIntoBatches(
                new ArrayList<>(foodMap.values()), orders);

        SupplyBatch batch = buildAndSaveBatch(batchGroups.get(0), date, 1, 1);

        log.info("[RE-AGGREGATE] Date={} → Created 1 new batch", date);

        return List.of(toResponse(batch));
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Get all batches
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> getAllBatches() {
        return supplyBatchRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. FLUSH EARLY - Gửi ngay cho Central không đợi cuối ngày
    //    Dùng khi có đơn gấp HIGH priority cần xử lý sớm
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse flushHighPriorityOrders(LocalDate date) {
        List<Order> highPriorityOrders = orderRepository
                .findByStatusOrderAndOrderDateAndPriorityLevel(
                        OrderStatus.WAITING_FOR_PRODUCTION, date, 1);

        if (highPriorityOrders.isEmpty()) {
            throw new IllegalStateException(
                    "Không có đơn HIGH priority nào với status WAITING_FOR_PRODUCTION trong ngày " + date);
        }

        Map<String, AggregatedFoodData> foodMap = aggregateFoods(highPriorityOrders);

        int totalTypes    = foodMap.size();
        int totalQuantity = foodMap.values().stream().mapToInt(f -> f.totalQty).sum();

        if (totalTypes > getMaxTypesPerDay() || totalQuantity > getMaxQuantityPerDay()) {
            throw new IllegalStateException(String.format(
                    "Đơn HIGH priority vượt giới hạn 1 batch: %d loại (max %d), %d món (max %d). " +
                            "Dùng aggregateDailyOrders để tách batch.",
                    totalTypes, getMaxTypesPerDay(), totalQuantity, getMaxQuantityPerDay()));
        }

        String batchId = "BATCH-URGENT-" + date + "-" + System.currentTimeMillis();
        SupplyBatch batch = SupplyBatch.builder()
                .batchId(batchId)
                .batchDate(date)
                .status(BatchStatus.SENT)
                .totalItems(totalQuantity)
                .totalTypes(totalTypes)
                .note("[URGENT] Flush sớm - " + highPriorityOrders.size() + " đơn HIGH priority")
                .sentAt(LocalDateTime.now())
                .build();

        foodMap.values().forEach(f -> {
            SupplyBatchItem item = SupplyBatchItem.builder()
                    .itemId(UUID.randomUUID().toString())
                    .centralFoodId(f.centralFoodId)
                    .foodName(f.foodName)
                    .totalQuantity(f.totalQty)
                    .sourceDetail(f.buildSourceDetailString())
                    .build();
            batch.addItem(item);
        });

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[FLUSH EARLY] Date={} | {} HIGH orders | {} types | {} items → SENT to central",
                date, highPriorityOrders.size(), totalTypes, totalQuantity);

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. SEND BATCH TO CENTRAL - Supply xác nhận gửi batch đến central
    //    Sau bước aggregate, batch ở trạng thái DRAFT.
    //    Supply review xong → gọi API này để gửi chính thức
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse sendBatchToCentral(String batchId) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể gửi batch ở trạng thái DRAFT. Trạng thái hiện tại: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.SENT);
        batch.setSentAt(LocalDateTime.now());

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[SEND] Batch {} → SENT to central kitchen at {}", batchId, saved.getSentAt());

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. UPDATE BATCH STATUS - Central kitchen cập nhật tiến độ
    //    SENT → IN_PRODUCTION → PRODUCTION_COMPLETED
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse updateBatchStatus(String batchId, BatchStatus newStatus) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        validateBatchStatusTransition(batch.getStatus(), newStatus);

        batch.setStatus(newStatus);
        SupplyBatch saved = supplyBatchRepository.save(batch);

        // Khi sản xuất hoàn thành → update tất cả orders liên quan
        if (newStatus == BatchStatus.PRODUCTION_COMPLETED) {
            updateRelatedOrdersToReadyToPick(saved);
        }

        log.info("[STATUS] Batch {} → {}", batchId, newStatus);
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. DEFER BATCH - Dời batch sang ngày khác
    //    Tình huống: Đơn LOW priority, store không cần gấp,
    //    dời sang tuần sau để không chiếm slot ngày hôm nay
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse deferBatch(String batchId, LocalDate newDate, String reason) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể dời batch ở trạng thái DRAFT. Hiện tại: " + batch.getStatus());
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày mới không thể là ngày trong quá khứ");
        }

        LocalDate oldDate = batch.getBatchDate();
        batch.setBatchDate(newDate);
        batch.setNote((batch.getNote() != null ? batch.getNote() + " | " : "")
                + "Dời từ " + oldDate + " → " + newDate + ". Lý do: " + reason);

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[DEFER] Batch {} dời từ {} → {}, lý do: {}", batchId, oldDate, newDate, reason);

        return toResponse(saved);
    }

    /**
     * Parse sourceDetail của từng item trong batch để lấy orderId,
     * sau đó update tất cả orders đó → READY_TO_PICK.
     *
     * sourceDetail format: "ORD006 (STORE-D1-001): 20, ORD007 (STORE-D2-001): 15"
     */
    private void updateRelatedOrdersToReadyToPick(SupplyBatch batch) {
        Set<String> orderIds = new HashSet<>();

        for (SupplyBatchItem item : batch.getItems()) {
            String sourceDetail = item.getSourceDetail();
            if (sourceDetail == null || sourceDetail.isBlank()) continue;

            for (String entry : sourceDetail.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("[")) continue;
                int spaceIdx = trimmed.indexOf(' ');
                if (spaceIdx > 0) {
                    orderIds.add(trimmed.substring(0, spaceIdx));
                }
            }
        }

        if (orderIds.isEmpty()) {
            log.warn("[PRODUCTION_COMPLETED] Batch {} không parse được orderId nào từ sourceDetail",
                    batch.getBatchId());
            return;
        }

        List<Order> orders = orderRepository.findAllById(orderIds);

        int updated = 0;
        for (Order order : orders) {
            if (order.getStatusOrder() == OrderStatus.WAITING_FOR_PRODUCTION) {
                order.setStatusOrder(OrderStatus.READY_TO_PICK);
                updated++;
            } else {
                log.warn("[PRODUCTION_COMPLETED] Order {} có status {} — bỏ qua, không update",
                        order.getOrderId(), order.getStatusOrder());
            }
        }

        orderRepository.saveAll(orders);
        log.info("[PRODUCTION_COMPLETED] Batch {} → updated {}/{} orders → READY_TO_PICK",
                batch.getBatchId(), updated, orderIds.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. GET BATCHES BY DATE
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> getBatchesByDate(LocalDate date) {
        return supplyBatchRepository.findByBatchDateAndStatus(date, BatchStatus.SENT).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. GET BATCHES BY STATUS
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> getBatchesByStatus(BatchStatus status) {
        return supplyBatchRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. GET BATCH BY ID
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse getBatchById(String batchId) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));
        return toResponse(batch);
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. CANCEL BATCH
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse cancelBatch(String batchId, String reason) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (batch.getStatus() == BatchStatus.IN_PRODUCTION
                || batch.getStatus() == BatchStatus.PRODUCTION_COMPLETED) {
            throw new IllegalStateException(
                    "Không thể huỷ batch đang sản xuất hoặc đã hoàn thành. Trạng thái: "
                            + batch.getStatus());
        }

        batch.setStatus(BatchStatus.CANCELLED);
        batch.setNote((batch.getNote() != null ? batch.getNote() + " | " : "")
                + "CANCELLED: " + reason);

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[CANCEL] Batch {} → CANCELLED. Lý do: {}", batchId, reason);

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 11. GET BATCHES BY CREATED AT DATE
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public List<SupplyBatchResponse> getBatchesByCreatedAt(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay   = date.plusDays(1).atStartOfDay();

        return supplyBatchRepository.findByCreatedAtBetween(startOfDay, endOfDay).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // 12. EDIT ITEM TRONG BATCH
    //     Chỉ cho phép edit khi batch đang DRAFT
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse editBatchItem(String batchId, String itemId,
                                             UpdateBatchItemRequest request) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể chỉnh sửa item khi batch ở trạng thái DRAFT. Hiện tại: "
                            + batch.getStatus());
        }

        SupplyBatchItem item = batch.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Item " + itemId + " không tồn tại trong batch " + batchId));

        CentralFoods centralFood = centralFoodsRepository.findById(request.getCentralFoodId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy món: " + request.getCentralFoodId()));

        String oldFoodId = item.getCentralFoodId();
        int    oldQty    = item.getTotalQuantity();

        item.setCentralFoodId(centralFood.getCentralFoodId());
        item.setFoodName(centralFood.getFoodName());
        item.setTotalQuantity(request.getQuantity());
        item.setSourceDetail("[Đã chỉnh thủ công bởi Supply] " + item.getSourceDetail());

        int diff          = request.getQuantity() - oldQty;
        int newTotalItems = batch.getTotalItems() + diff;

        if (newTotalItems > getMaxQuantityPerDay()) {
            throw new IllegalArgumentException(String.format(
                    "Không thể chỉnh sửa: tổng số món sau khi edit là %d, vượt giới hạn %d món/ngày.",
                    newTotalItems, getMaxQuantityPerDay()));
        }

        batch.setTotalItems(newTotalItems);

        if (!oldFoodId.equals(request.getCentralFoodId())) {
            boolean oldFoodStillExists = batch.getItems().stream()
                    .anyMatch(i -> i.getCentralFoodId().equals(oldFoodId)
                            && !i.getItemId().equals(itemId));
            boolean newFoodAlreadyExists = batch.getItems().stream()
                    .anyMatch(i -> i.getCentralFoodId().equals(request.getCentralFoodId())
                            && !i.getItemId().equals(itemId));

            int typesDelta = 0;
            if (!oldFoodStillExists)   typesDelta--;
            if (!newFoodAlreadyExists) typesDelta++;

            int newTotalTypes = batch.getTotalTypes() + typesDelta;

            if (newTotalTypes > getMaxTypesPerDay()) {
                throw new IllegalArgumentException(String.format(
                        "Không thể chỉnh sửa: tổng số loại món sau khi edit là %d, vượt giới hạn %d loại/ngày.",
                        newTotalTypes, getMaxTypesPerDay()));
            }

            batch.setTotalTypes(newTotalTypes);
        }

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[EDIT ITEM] Batch {} | item {} | food: {} → {} | qty: {} → {}",
                batchId, itemId, oldFoodId, request.getCentralFoodId(), oldQty, request.getQuantity());

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 13. REMOVE ITEM KHỎI BATCH
    //     Chỉ cho phép xóa khi batch đang DRAFT
    //     Không cho xóa nếu batch chỉ còn 1 item
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse removeItemFromBatch(String batchId, String itemId) {
        SupplyBatch batch = supplyBatchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể xóa item khi batch ở trạng thái DRAFT. Hiện tại: "
                            + batch.getStatus());
        }

        SupplyBatchItem item = batch.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Item " + itemId + " không tồn tại trong batch " + batchId));

        if (batch.getItems().size() == 1) {
            throw new IllegalStateException(
                    "Batch chỉ còn 1 item, không thể xóa. Dùng API cancel batch nếu muốn hủy cả batch.");
        }

        batch.setTotalItems(batch.getTotalItems() - item.getTotalQuantity());
        batch.setTotalTypes(batch.getTotalTypes() - 1);
        batch.getItems().remove(item);

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[REMOVE ITEM] Batch {} → removed item {} (food: {}, qty: {})",
                batchId, itemId, item.getCentralFoodId(), item.getTotalQuantity());

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // 14. CREATE MANUAL BATCH
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SupplyBatchResponse createManualBatch(CreateBatchRequest request) {
        int totalTypes    = request.getItems().size();
        int totalQuantity = request.getItems().stream()
                .mapToInt(CreateBatchRequest.BatchItemRequest::getQuantity).sum();

        if (totalTypes > getMaxTypesPerDay()) {
            throw new IllegalArgumentException(String.format(
                    "Vượt giới hạn %d loại món/ngày. Hiện tại: %d loại.",
                    getMaxTypesPerDay(), totalTypes));
        }
        if (totalQuantity > getMaxQuantityPerDay()) {
            throw new IllegalArgumentException(String.format(
                    "Vượt giới hạn %d món/ngày. Hiện tại: %d món.",
                    getMaxQuantityPerDay(), totalQuantity));
        }

        String batchId = "BATCH-MANUAL-" + request.getBatchDate() + "-" + System.currentTimeMillis();

        SupplyBatch batch = SupplyBatch.builder()
                .batchId(batchId)
                .batchDate(request.getBatchDate())
                .status(BatchStatus.DRAFT)
                .totalItems(totalQuantity)
                .totalTypes(totalTypes)
                .note(request.getNote() != null
                        ? "[Manual] " + request.getNote()
                        : "[Manual] Tạo thủ công bởi Supply")
                .build();

        for (CreateBatchRequest.BatchItemRequest itemReq : request.getItems()) {
            CentralFoods food = centralFoodsRepository.findById(itemReq.getCentralFoodId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy món: " + itemReq.getCentralFoodId()));

            SupplyBatchItem item = SupplyBatchItem.builder()
                    .itemId(UUID.randomUUID().toString())
                    .centralFoodId(food.getCentralFoodId())
                    .foodName(food.getFoodName())
                    .totalQuantity(itemReq.getQuantity())
                    .sourceDetail("[Manual] Supply tạo thủ công")
                    .build();
            batch.addItem(item);
        }

        SupplyBatch saved = supplyBatchRepository.save(batch);
        log.info("[MANUAL BATCH] Created {} | date={} | {} types | {} items",
                batchId, request.getBatchDate(), totalTypes, totalQuantity);

        return toResponse(saved);
    }

    // ─── Private helpers ──────────────────────────────────────────

    /**
     * Gom tất cả order items theo foodId.
     * Cùng món từ nhiều stores → cộng số lượng, ghi rõ nguồn (orderId + storeId).
     */
    private Map<String, AggregatedFoodData> aggregateFoods(List<Order> orders) {
        Map<String, AggregatedFoodData> foodMap = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getOrderDetail() == null) continue;

            for (OrderDetailItem item : order.getOrderDetail().getOrderDetailItems()) {
                String foodId = item.getCentralFoodId();

                AggregatedFoodData agg = foodMap.computeIfAbsent(foodId, k ->
                        AggregatedFoodData.builder()
                                .centralFoodId(foodId)
                                .foodName(item.getFoodName())
                                .totalQty(0)
                                .sourceMap(new LinkedHashMap<>())
                                .build()
                );

                agg.totalQty += item.getQuantity();

                String sourceKey = order.getOrderId() + " (" + order.getStoreId() + ")";
                agg.sourceMap.merge(sourceKey, item.getQuantity(), Integer::sum);
            }
        }

        return foodMap;
    }

    /**
     * Chia danh sách món thành các batch theo giới hạn.
     * Với logic mới, input đã được validate ≤ giới hạn nên luôn trả về đúng 1 batch.
     * Giữ nguyên để flushHighPriorityOrders có thể dùng chung.
     */
    private List<List<AggregatedFoodData>> splitIntoBatches(
            List<AggregatedFoodData> foods, List<Order> orders) {

        Set<String> highPriorityFoodIds = getHighPriorityFoodIds(orders);
        foods.sort((a, b) -> {
            boolean aHigh = highPriorityFoodIds.contains(a.centralFoodId);
            boolean bHigh = highPriorityFoodIds.contains(b.centralFoodId);
            if (aHigh != bHigh) return aHigh ? -1 : 1;
            return Integer.compare(b.totalQty, a.totalQty);
        });

        List<List<AggregatedFoodData>> result     = new ArrayList<>();
        List<AggregatedFoodData>       currentBatch = new ArrayList<>();
        int currentQty = 0;

        for (AggregatedFoodData food : foods) {
            int remaining = food.totalQty;

            while (remaining > 0) {
                int qtySlot  = getMaxQuantityPerDay() - currentQty;
                int typeSlot = getMaxTypesPerDay()    - currentBatch.size();

                boolean canFit = qtySlot > 0 && typeSlot > 0;

                if (!canFit) {
                    result.add(currentBatch);
                    currentBatch = new ArrayList<>();
                    currentQty   = 0;
                    qtySlot      = getMaxQuantityPerDay();
                }

                int take = Math.min(remaining, qtySlot);

                AggregatedFoodData existing = currentBatch.stream()
                        .filter(f -> f.centralFoodId.equals(food.centralFoodId))
                        .findFirst().orElse(null);

                if (existing != null) {
                    existing.totalQty += take;
                } else {
                    AggregatedFoodData slice = AggregatedFoodData.builder()
                            .centralFoodId(food.centralFoodId)
                            .foodName(food.foodName)
                            .totalQty(take)
                            .sourceMap(food.sourceMap)
                            .build();
                    currentBatch.add(slice);
                }

                currentQty += take;
                remaining  -= take;
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }

        return result;
    }

    /**
     * Lấy tập foodId có trong đơn HIGH priority (priorityLevel = 1)
     */
    private Set<String> getHighPriorityFoodIds(List<Order> orders) {
        Set<String> result = new HashSet<>();
        for (Order order : orders) {
            if (Integer.valueOf(1).equals(order.getPriorityLevel())
                    && order.getOrderDetail() != null) {
                order.getOrderDetail().getOrderDetailItems()
                        .forEach(i -> result.add(i.getCentralFoodId()));
            }
        }
        return result;
    }

    private SupplyBatch buildAndSaveBatch(
            List<AggregatedFoodData> group, LocalDate batchDate,
            int batchIndex, int totalBatches) {

        int totalQty   = group.stream().mapToInt(f -> f.totalQty).sum();
        int totalTypes = group.size();

        String batchId = "BATCH-" + batchDate + "-" + batchIndex;
        String note = totalBatches > 1
                ? String.format("Batch %d/%d của ngày %s", batchIndex, totalBatches, batchDate)
                : "Batch đơn ngày " + batchDate;

        SupplyBatch batch = SupplyBatch.builder()
                .batchId(batchId)
                .batchDate(batchDate)
                .status(BatchStatus.DRAFT)
                .totalItems(totalQty)
                .totalTypes(totalTypes)
                .note(note)
                .build();

        for (AggregatedFoodData f : group) {
            SupplyBatchItem item = SupplyBatchItem.builder()
                    .itemId(UUID.randomUUID().toString())
                    .centralFoodId(f.centralFoodId)
                    .foodName(f.foodName)
                    .totalQuantity(f.totalQty)
                    .sourceDetail(f.buildSourceDetailString())
                    .build();
            batch.addItem(item);
        }

        return supplyBatchRepository.save(batch);
    }

    private void validateBatchStatusTransition(BatchStatus current, BatchStatus next) {
        Map<BatchStatus, Set<BatchStatus>> allowed = Map.of(
                BatchStatus.DRAFT,                Set.of(BatchStatus.SENT, BatchStatus.CANCELLED),
                BatchStatus.SENT,                 Set.of(BatchStatus.IN_PRODUCTION, BatchStatus.CANCELLED),
                BatchStatus.IN_PRODUCTION,        Set.of(BatchStatus.PRODUCTION_COMPLETED),
                BatchStatus.PRODUCTION_COMPLETED, Set.of(),
                BatchStatus.CANCELLED,            Set.of()
        );

        if (!allowed.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException(
                    "Không thể chuyển batch từ " + current + " → " + next);
        }
    }

    private SupplyBatchResponse toResponse(SupplyBatch batch) {
        List<SupplyBatchItemResponse> items = batch.getItems().stream()
                .map(item -> SupplyBatchItemResponse.builder()
                        .itemId(item.getItemId())
                        .centralFoodId(item.getCentralFoodId())
                        .foodName(item.getFoodName())
                        .totalQuantity(item.getTotalQuantity())
                        .sourceDetail(item.getSourceDetail())
                        .build())
                .collect(Collectors.toList());

        return SupplyBatchResponse.builder()
                .batchId(batch.getBatchId())
                .batchDate(batch.getBatchDate())
                .status(batch.getStatus())
                .totalItems(batch.getTotalItems())
                .totalTypes(batch.getTotalTypes())
                .note(batch.getNote())
                .createdAt(batch.getCreatedAt())
                .sentAt(batch.getSentAt())
                .items(items)
                .build();
    }

    // ─── Inner helper class ───────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    private static class AggregatedFoodData {
        String centralFoodId;
        String foodName;
        int totalQty;
        Map<String, Integer> sourceMap; // "orderId (storeId)" → quantity

        String buildSourceDetailString() {
            if (sourceMap == null || sourceMap.isEmpty()) return "";
            return sourceMap.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));
        }
    }
}