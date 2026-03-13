package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentMethodResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentRecordResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.StoreResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreatePaymentRecordRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateStoreRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdatePaymentMethodRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStore;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentRecord;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.User;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStorePaymentMethodRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStorePaymentRecordRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStoreRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FranchiseStoreService {

    private final FranchiseStoreRepository franchiseStoreRepository;
    private final UserRepository userRepository;
    private final FranchiseStorePaymentRecordRepository paymentRecordRepository;
    private final FranchiseStorePaymentMethodRepository paymentMethodRepository;

    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        String storeId = generateStoreId(); // ✅ Tự generate

        FranchiseStore store = FranchiseStore.builder()
                .storeId(storeId)
                .storeName(request.storeName())
                .address(request.address())
                .district(request.district())
                .ward(request.ward())
                .revenue(request.revenue())
                .numberOfContact(request.numberOfContact())
                .deptStatus(false)
                .build();

        if (request.managerEmail() != null && !request.managerEmail().isBlank()) {
            User manager = userRepository.findByEmail(request.managerEmail())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy user với email: " + request.managerEmail()));
            store.assignManager(manager);
        }

        FranchiseStore saved = franchiseStoreRepository.save(store);

        FranchiseStorePaymentMethod defaultMethod = new FranchiseStorePaymentMethod();
        defaultMethod.setStorePaymentId(UUID.randomUUID().toString());
        defaultMethod.setStoreId(saved.getStoreId());
        defaultMethod.setPaymentMethod("CREDIT");
        paymentMethodRepository.save(defaultMethod);

        return toStoreResponse(saved);
    }

    private String generateStoreId() {
        long count = franchiseStoreRepository.count() + 1;
        int random = (int)(Math.random() * 900) + 100; // 3 số random 100-999
        return "STORE-D" + count + "-" + random;
    }


    // ── GET STORE ─────────────────────────────────────────────────────────────────
    @Transactional
    public StoreResponse getStore(String storeId) {
        return toStoreResponse(findStoreOrThrow(storeId));
    }


    @Transactional
    public List<StoreResponse> getAllStores() {
        return franchiseStoreRepository.findAll()
                .stream()
                .map(this::toStoreResponse)
                .collect(Collectors.toList());
    }

    // ── PAYMENT METHOD ────────────────────────────────────────────────────────────

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(String storeId,
                                                     UpdatePaymentMethodRequest request) {
        if (request.paymentMethod() == null) {
            throw new IllegalArgumentException("paymentMethod must be CASH or CREDIT.");
        }

        findStoreOrThrow(storeId);

        String methodValue = request.paymentMethod().name();

        return paymentMethodRepository
                .findByStoreIdAndPaymentMethod(storeId, methodValue)
                .map(this::toPaymentMethodResponse)
                .orElseGet(() -> {
                    FranchiseStorePaymentMethod pm = new FranchiseStorePaymentMethod();
                    pm.setStorePaymentId(UUID.randomUUID().toString());
                    pm.setStoreId(storeId);
                    pm.setPaymentMethod(methodValue);
                    return toPaymentMethodResponse(paymentMethodRepository.save(pm));
                });
    }

    // ── PAYMENT RECORD ────────────────────────────────────────────────────────────

    @Transactional
    public PaymentRecordResponse addPaymentRecord(CreatePaymentRecordRequest request) {

        FranchiseStore store = findStoreOrThrow(request.storeId());
        if (!store.isDeptStatus()) {
            throw new IllegalStateException("Store này không có nợ, không thể nhập payment record.");
        }

        if (request.debtAmount() == null || request.debtAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("debtAmount must be >= 0.");
        }

        // 0 → no debt (false); > 0 → has debt (true)
        store.setDeptStatus(request.debtAmount().compareTo(BigDecimal.ZERO) > 0);
        franchiseStoreRepository.save(store);

        FranchiseStorePaymentRecord record = FranchiseStorePaymentRecord.builder()
                .paymentRecordId(UUID.randomUUID().toString())
                .storeId(request.storeId())
                .debtAmount(request.debtAmount())
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        return toPaymentRecordResponse(paymentRecordRepository.save(record));
    }

    public List<PaymentRecordResponse> getPaymentRecords(String storeId) {
        findStoreOrThrow(storeId);
        return paymentRecordRepository
                .findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream()
                .map(this::toPaymentRecordResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StoreResponse payDebtByCash(String storeId) {
        FranchiseStore store = findStoreOrThrow(storeId);

        // ✅ Check paymentMethod của store phải là CASH
        List<String> storeMethods = paymentMethodRepository.findByStoreId(storeId)
                .stream()
                .map(FranchiseStorePaymentMethod::getPaymentMethod)
                .collect(Collectors.toList());

        if (!storeMethods.contains("CASH")) {
            throw new IllegalStateException(
                    "Store " + storeId + " không hỗ trợ CASH. Không thể thanh toán tiền mặt.");
        }

        if (!store.isDeptStatus()) {
            throw new IllegalStateException("Store " + storeId + " không có nợ cần thanh toán");
        }

        if (!store.isDeptStatus()) {
            throw new IllegalStateException("Store " + storeId + " không có nợ cần thanh toán");
        }

        List<FranchiseStorePaymentRecord> debtRecords =
                paymentRecordRepository.findByStoreId(storeId);
        debtRecords.forEach(r -> {
            r.setStatus("PAID");
            paymentRecordRepository.save(r);
        });

        // Reset deptStatus = false
        store.setDeptStatus(false);
        franchiseStoreRepository.save(store);

        //log.info("Store {} debt paid by CASH", storeId);
        return toStoreResponse(store);
    }

    @Transactional
    public StoreResponse updateDebtStatus(String storeId, boolean debtStatus) {
        FranchiseStore store = findStoreOrThrow(storeId);
        store.setDeptStatus(debtStatus);
        franchiseStoreRepository.save(store);
        return toStoreResponse(store);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────────

    private FranchiseStore findStoreOrThrow(String storeId) {
        return franchiseStoreRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
    }

    private StoreResponse toStoreResponse(FranchiseStore s) {
        List<String> methods = paymentMethodRepository.findByStoreId(s.getStoreId())
                .stream()
                .map(FranchiseStorePaymentMethod::getPaymentMethod)
                .collect(Collectors.toList());

        return new StoreResponse(
                s.getStoreId(),
                s.getStoreName(),
                s.getAddress(),
                s.getDistrict(),
                s.getWard(),
                s.isDeptStatus(),
                s.getRevenue(),
                s.getNumberOfContact(),
                s.getManager() != null ? s.getManager().getEmail() : null,
                methods
        );
    }

    private PaymentRecordResponse toPaymentRecordResponse(FranchiseStorePaymentRecord r) {
        return new PaymentRecordResponse(
                r.getPaymentRecordId(),
                r.getStoreId(),
                r.getDebtAmount(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }

    private PaymentMethodResponse toPaymentMethodResponse(FranchiseStorePaymentMethod pm) {
        return new PaymentMethodResponse(
                pm.getStorePaymentId(),
                pm.getStoreId(),
                pm.getPaymentMethod()
        );
    }
}
