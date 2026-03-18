package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AggregatePreviewResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.SupplyBatchResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.BatchStatus;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    // ─────────────────────────────────────────────────────────────
    // 1. PREVIEW tổng hợp cuối ngày
    //    SUPPLY_COORDINATOR xem trước trước khi aggregate
    //    CENTRAL_STAFF không cần xem — họ chỉ nhận batch đã gửi
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "tổng hợp đơn theo ngày, gợi ý phân các lô sản xuất (nhập theo format yyyy-mm-dd)")
    public ResponseEntity<AggregatePreviewResponse> previewAggregation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.previewAggregation(date));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. AGGREGATE cuối ngày → tạo batch(es) DRAFT
    //    Chỉ SUPPLY_COORDINATOR mới được tổng hợp đơn
    //    CENTRAL_STAFF không có quyền tạo batch
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/aggregate")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "xem tổng hợp xong thấy ưng ý thì tạo lô sản xuất")
    public ResponseEntity<List<SupplyBatchResponse>> aggregateDailyOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.aggregateDailyOrders(date));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. FLUSH EARLY - Gửi ngay đơn HIGH priority
    //    Chỉ SUPPLY_COORDINATOR mới được quyết định flush sớm
    //    Đây là quyết định điều phối, không phải việc của central
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/flush-urgent")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Gửi ngay đơn HIGH priority")
    public ResponseEntity<SupplyBatchResponse> flushHighPriorityOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.flushHighPriorityOrders(date));
    }

    // ─────────────────────────────────────────────────────────────
    // 4. SEND batch → Central Kitchen (DRAFT → SENT)
    //    Chỉ SUPPLY_COORDINATOR mới được gửi batch cho central
    //    Central không tự lấy batch — phải được supply gửi
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/batches/{batchId}/send")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Gửi lô sản xuất cho central kitchen")
    public ResponseEntity<SupplyBatchResponse> sendBatchToCentral(
            @PathVariable String batchId) {

        return ResponseEntity.ok(supplyService.sendBatchToCentral(batchId));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. UPDATE STATUS batch (SENT → IN_PRODUCTION → DELIVERED)
    //    CENTRAL_STAFF được quyền cập nhật tiến độ sản xuất
    //    SUPPLY_COORDINATOR cũng được — để override nếu cần
    //    Các transition không hợp lệ đã bị chặn ở service layer
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/batches/{batchId}/status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Cập nhật status của lô")
    public ResponseEntity<SupplyBatchResponse> updateBatchStatus(
            @PathVariable String batchId,
            @RequestParam BatchStatus newStatus) {

        return ResponseEntity.ok(supplyService.updateBatchStatus(batchId, newStatus));
    }

    // ─────────────────────────────────────────────────────────────
    // 6. DEFER batch sang ngày khác
    //    Chỉ SUPPLY_COORDINATOR mới được dời lịch sản xuất
    //    CENTRAL_STAFF không có quyền thay đổi lịch — họ chỉ làm theo
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/batches/{batchId}/defer")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Dời lịch sx của lô sang ngày khác")
    public ResponseEntity<SupplyBatchResponse> deferBatch(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam(required = false, defaultValue = "") String reason) {

        return ResponseEntity.ok(supplyService.deferBatch(batchId, newDate, reason));
    }

    // ─────────────────────────────────────────────────────────────
    // 7. GET batches theo ngày
    //    Cả SUPPLY lẫn CENTRAL đều cần xem —
    //    supply để quản lý, central để biết hôm nay cần làm gì
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Central kitchen xem lô hôm nay cần sản xuất những gì")
    public ResponseEntity<List<SupplyBatchResponse>> getBatchesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.getBatchesByDate(date));
    }

    // ─────────────────────────────────────────────────────────────
    // 8. GET batches theo status
    //    Cả hai đều cần — central lọc SENT để biết batch nào cần làm,
    //    supply lọc DRAFT để biết batch nào chưa gửi
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/batches/by-status")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Tìm lô theo status")
    public ResponseEntity<List<SupplyBatchResponse>> getBatchesByStatus(
            @RequestParam BatchStatus status) {

        return ResponseEntity.ok(supplyService.getBatchesByStatus(status));
    }

    // ─────────────────────────────────────────────────────────────
    // 9. GET batch theo ID
    //    Cả hai đều cần xem chi tiết 1 batch cụ thể
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Tìm lô theo id")
    public ResponseEntity<SupplyBatchResponse> getBatchById(
            @PathVariable String batchId) {

        return ResponseEntity.ok(supplyService.getBatchById(batchId));
    }

    // ─────────────────────────────────────────────────────────────
    // 10. CANCEL batch
    //     Chỉ SUPPLY_COORDINATOR mới được hủy batch
    //     CENTRAL_STAFF không được hủy — nếu có vấn đề phải báo supply
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Hủy lô sản xuất")
    public ResponseEntity<SupplyBatchResponse> cancelBatch(
            @PathVariable String batchId,
            @RequestParam(required = false, defaultValue = "") String reason) {

        return ResponseEntity.ok(supplyService.cancelBatch(batchId, reason));
    }

    // GET /supply/batches/all
    @GetMapping("/batches/all")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "get all batches")
    public ResponseEntity<List<SupplyBatchResponse>> getAllBatches() {
        return ResponseEntity.ok(supplyService.getAllBatches());
    }

    @GetMapping("/batches/by-created-at")
    @PreAuthorize("hasAnyRole('SUPPLY_COORDINATOR', 'CENTRAL_STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupplyBatchResponse>> getBatchesByCreatedAt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(supplyService.getBatchesByCreatedAt(date));
    }
}