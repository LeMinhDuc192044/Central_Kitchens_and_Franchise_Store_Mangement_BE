package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AggregatePreviewResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.SupplyBatchResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateBatchRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdateBatchItemRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/supply")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SupplyController {

    private final SupplyService supplyService;


    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Xem tổng hợp đơn theo ngày tạo, gợi ý phân lô sản xuất (format yyyy-MM-dd)")
    public ResponseEntity<AggregatePreviewResponse> previewAggregation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.previewAggregation(date));
    }



    @PostMapping("/aggregate")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo lô sản xuất từ các đơn trong ngày (tạo DRAFT)")
    public ResponseEntity<List<SupplyBatchResponse>> aggregateDailyOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.aggregateDailyOrders(date));
    }

    @PostMapping("/re-aggregate")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Xóa lô DRAFT cũ và tạo lại — dùng khi muốn làm lại sau aggregate")
    public ResponseEntity<List<SupplyBatchResponse>> reAggregateDailyOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.reAggregateDailyOrders(date));
    }

    @PostMapping("/batches/{batchId}/send")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Gửi lô sản xuất cho central kitchen (DRAFT → SENT)")
    public ResponseEntity<SupplyBatchResponse> sendBatchToCentral(
            @PathVariable String batchId) {

        return ResponseEntity.ok(supplyService.sendBatchToCentral(batchId));
    }


    @PatchMapping("/batches/{batchId}/status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Cập nhật trạng thái lô: SENT → IN_PRODUCTION → DELIVERED")
    public ResponseEntity<SupplyBatchResponse> updateBatchStatus(
            @PathVariable String batchId,
            @RequestParam BatchStatus newStatus) {

        return ResponseEntity.ok(supplyService.updateBatchStatus(batchId, newStatus));
    }


    @GetMapping("/batches/all")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN', 'CENTRAL_KITCHEN_STAFF')")
    @Operation(summary = "Lấy tất cả lô sản xuất")
    public ResponseEntity<List<SupplyBatchResponse>> getAllBatches() {
        return ResponseEntity.ok(supplyService.getAllBatches());
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Central Lấy lô theo ngày sản xuất (batchDate, format yyyy-MM-dd)")
    public ResponseEntity<List<SupplyBatchResponse>> getBatchesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.getBatchesByDate(date));
    }


    @GetMapping("/batches/by-status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Supply Lấy lô theo status")
    public ResponseEntity<List<SupplyBatchResponse>> getBatchesByStatus(
            @RequestParam BatchStatus status) {
        return ResponseEntity.ok(supplyService.getBatchesByStatus(status));
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Supply Lấy chi tiết lô theo ID")
    public ResponseEntity<SupplyBatchResponse> getBatchById(
            @PathVariable String batchId) {
        return ResponseEntity.ok(supplyService.getBatchById(batchId));
    }

    @PatchMapping("/batches/{batchId}/defer")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Dời lịch sản xuất sang ngày khác (chỉ DRAFT)")
    public ResponseEntity<SupplyBatchResponse> deferBatch(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ResponseEntity.ok(supplyService.deferBatch(batchId, newDate, reason));
    }

    @DeleteMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Hủy lô sản xuất (chỉ DRAFT hoặc SENT)")
    public ResponseEntity<SupplyBatchResponse> cancelBatch(
            @PathVariable String batchId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ResponseEntity.ok(supplyService.cancelBatch(batchId, reason));
    }

    @PatchMapping("/batches/{batchId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Chỉnh sửa item trong lô DRAFT (centralFoodId và quantity)")
    public ResponseEntity<SupplyBatchResponse> editBatchItem(
            @PathVariable String batchId,
            @PathVariable String itemId,
            @RequestBody @Valid UpdateBatchItemRequest request) {

        return ResponseEntity.ok(supplyService.editBatchItem(batchId, itemId, request));
    }

    @DeleteMapping("/batches/{batchId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Xóa item khỏi lô DRAFT — nếu còn 1 item thì dùng cancel batch")
    public ResponseEntity<SupplyBatchResponse> removeItemFromBatch(
            @PathVariable String batchId,
            @PathVariable String itemId) {

        return ResponseEntity.ok(supplyService.removeItemFromBatch(batchId, itemId));
    }

    @PostMapping("/batches/manual")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo lô sản xuất thủ công — supply tự chọn món và số lượng")
    public ResponseEntity<SupplyBatchResponse> createManualBatch(
            @RequestBody @Valid CreateBatchRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplyService.createManualBatch(request));
    }
}