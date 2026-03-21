package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentMethodResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.PaymentRecordResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.StoreResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreatePaymentRecordRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CreateStoreRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.UpdatePaymentMethodRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStore;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentRecord;
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
    private final GhnAddressValidationService ghnAddressValidationService;

    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        String storeId = generateStoreId();

        GhnAddressValidationService.GhnAddressResult ghnAddress = ghnAddressValidationService.validateFullAddressById(
                request.province(),
                request.district(),
                request.ward()
        );


        FranchiseStore store = FranchiseStore.builder()
                .storeId(storeId)
                .storeName(request.storeName())
                .address(request.address())       // raw street address
                .district(request.district())
                .province(request.province())
                .numberOfContact(request.numberOfContact())
                .ward(request.ward())
                .district(ghnAddress.getDistrictId())  // ← GHN district ID
                .ward(ghnAddress.getWardCode())       // ← GHN ward code
                .numberOfContact(null)   // sẽ được fill khi staff đăng ký
                .deptStatus(false)
                .build();

        FranchiseStore saved = franchiseStoreRepository.save(store);


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


        return new StoreResponse(
                s.getStoreId(),
                s.getStoreName(),
                s.getAddress(),
                s.getDistrict(),
                s.getWard(),
                s.getProvince(),
                s.isDeptStatus(),
                s.getNumberOfContact()
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


}
