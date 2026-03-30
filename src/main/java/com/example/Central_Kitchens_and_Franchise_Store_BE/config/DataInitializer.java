package com.example.Central_Kitchens_and_Franchise_Store_BE.config;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.*;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FranchiseStoreRepository franchiseStoreRepository;
    private final CentralFoodCategoryRepository foodCategoryRepository;
    private final CentralFoodsRepository centralFoodRepository;
    private final KitchenConfigRepository kitchenConfigRepository;
    private final OrderRepository orderRepository;

    @Bean
    CommandLineRunner initTestUser() {
        return args -> {

            createRoleIfNotExists(UserRole.ADMIN);
            createRoleIfNotExists(UserRole.SUPPLY_COORDINATOR);
            createRoleIfNotExists(UserRole.CENTRAL_KITCHEN_STAFF);
            createRoleIfNotExists(UserRole.MANAGER);
            createRoleIfNotExists(UserRole.FRANCHISE_STAFF);

            createUserIfNotExists(
                    "admin@centralkitchen.com",
                    "System Administrator",
                    "0901234567",
                    UserRole.ADMIN
            );

            createUserIfNotExists(
                    "manager@centralkitchen.com",
                    "Operations Manager",
                    "0902345678",
                    UserRole.MANAGER
            );

            createUserIfNotExists(
                    "supply@centralkitchen.com",
                    "Supply Coordinator",
                    "0905678901",
                    UserRole.SUPPLY_COORDINATOR
            );

            createUserIfNotExists(
                    "kitchen@centralkitchen.com",
                    "Head Chef",
                    "0904567890",
                    UserRole.CENTRAL_KITCHEN_STAFF
            );

            createUserIfNotExists(
                    "store1.manager@centralkitchen.com",
                    "District 1 Store Manager",
                    "0903456790",
                    UserRole.FRANCHISE_STAFF
            );

            createUserIfNotExists(
                    "store2.manager@centralkitchen.com",
                    "District 2 Store Manager",
                    "0903456791",
                    UserRole.FRANCHISE_STAFF
            );

            createUserIfNotExists(
                    "store3.manager@centralkitchen.com",
                    "District 3 Store Manager",
                    "0903456792",
                    UserRole.FRANCHISE_STAFF
            );

            // ── Store 1 - District 1 ──────────────────────────────
            createStoreIfNotExists(
                    "STORE-D1-001",
                    "Central Kitchen District 1",
                    "123 Nguyễn Huệ",
                    "Hồ Chí Minh",
                    1452,           // ← GHN district ID for Quận 1
                    "21001",        // ← GHN ward code for Bến Nghé
                    "Bến Nghé",
                    "store1.manager@centralkitchen.com"
            );


// ── Store 2 - Quận 2, Hồ Chí Minh ────────────────────────────────
            createStoreIfNotExists(
                    "STORE-D2-001",
                    "Central Kitchen District 2",
                    "456 Trần Não",
                    "Hồ Chí Minh",
                    1444,           // ← GHN district ID for Quận 2
                    "20308",        // ← GHN ward code for An Khánh
                    "An Khánh",
                    "store2.manager@centralkitchen.com"
            );


// ── Store 3 - Quận 3, Hồ Chí Minh ────────────────────────────────
            createStoreIfNotExists(
                    "STORE-D3-001",
                    "Central Kitchen District 3",
                    "789 Võ Văn Tần",
                    "Hồ Chí Minh",
                    1446,           // ← GHN district ID for Quận 3
                    "20406",        // ← GHN ward code for Phường 6
                    "Phường 6",
                    "store3.manager@centralkitchen.com"
            );


            createOrderIfNotExists("ORD001", 1, "Test order 1",           "PAY_AFTER_ORDER",         "CASH",   "SUCCESS",        LocalDate.of(2026,3,5),  "DELIVERED",         null,                    "STORE-D1-001", LocalDateTime.of(2026,3,5,8,30,0));
            createOrderIfNotExists("ORD002", 2, "Test order 2",           "PAY_AFTER_DELIVERY",      "CREDIT", "SUCCESS",        LocalDate.of(2026,3,12), "COMPLETED",         null,                    "STORE-D1-001", LocalDateTime.of(2026,3,12,11,45,0));
            createOrderIfNotExists("ORD003", 3, "Test order 3 cancelled", "PAY_AFTER_ORDER",         "CASH",   "CANCELLED",      LocalDate.of(2026,3,18), "CANCELLED",         "Customer changed mind",  "STORE-D1-001", LocalDateTime.of(2026,3,18,14,20,0));
            createOrderIfNotExists("ORD004", 1, "Test order 4",           "PAY_AT_THE_END_OF_MONTH", "CREDIT", "PENDING",        LocalDate.of(2026,3,25), "IN_PROGRESS",       null,                    "STORE-D1-001", LocalDateTime.of(2026,3,25,19,55,0));
            createOrderIfNotExists("ORD005", 2, "Test order 5",           "PAY_AFTER_ORDER",         "CREDIT", "SUCCESS",        LocalDate.of(2026,3,8),  "DELIVERED",         null,                    "STORE-D2-001", LocalDateTime.of(2026,3,8,9,10,0));
            createOrderIfNotExists("ORD006", 1, "Test order 6",           "PAY_AFTER_DELIVERY",      "CASH",   "SUCCESS",        LocalDate.of(2026,2,14), "COMPLETED",         null,                    "STORE-D2-001", LocalDateTime.of(2026,2,14,13,30,0));
            createOrderIfNotExists("ORD007", 3, "Test order 7",           "PAY_AT_THE_END_OF_MONTH", "CREDIT", "PENDING",        LocalDate.of(2026,3,20), "COOKING_DONE",      null,                    "STORE-D2-001", LocalDateTime.of(2026,3,20,17,45,0));
            createOrderIfNotExists("ORD008", 1, "Test order 8",           "PAY_AFTER_ORDER",         "CASH",   "SUCCESS",        LocalDate.of(2026,3,1),  "DELIVERED",         null,                    "STORE-D3-001", LocalDateTime.of(2026,3,1,10,0,0));
            createOrderIfNotExists("ORD009", 2, "Test order 9",           "PAY_AFTER_DELIVERY",      "CREDIT", "PENDING_REFUND", LocalDate.of(2026,2,20), "WAITING_TO_RETURN", null,                    "STORE-D3-001", LocalDateTime.of(2026,2,20,15,20,0));
            createOrderIfNotExists("ORD010", 3, "Test order 10 cancelled","PAY_AFTER_ORDER",         "CASH",   "CANCELLED",      LocalDate.of(2026,3,15), "CANCELLED",         "Out of stock",          "STORE-D3-001", LocalDateTime.of(2026,3,15,21,30,0));
            // ── Food Categories ──────────────────────────────
            createCategoryIfNotExists("CE_CH_482917", "Chicken");
            createCategoryIfNotExists("CE_NO_739204", "Noodle");
            createCategoryIfNotExists("CE_CA_156893", "Cake");
            createCategoryIfNotExists("CE_BU_904561", "Burger");

// ── Foods: Gà ────────────────────────────────────
            createFoodIfNotExists("CE_CH_FO_000001", "Gà Phô Mai",                50, 65000, "CE_CH_482917", 2,  3, 5, 5);
            createFoodIfNotExists("CE_CH_FO_000002", "Cánh Gà Chiên Mắm Tỏi Ớt", 50, 70000, "CE_CH_482917", 4,  4, 3, 3);
            createFoodIfNotExists("CE_CH_FO_000003", "Gà Sốt Teriyaki",           50, 72000, "CE_CH_482917", 5,  5, 6, 4);

// ── Foods: Mỳ Ý ──────────────────────────────────
            createFoodIfNotExists("CE_NO_FO_000001", "Mỳ Ý Sốt Bò Bằm",          50, 68000, "CE_NO_739204", 5,  4, 5, 8);
            createFoodIfNotExists("CE_NO_FO_000002", "Mỳ Ý Cua",                  50, 75000, "CE_NO_739204", 5,  5, 6, 7);
            createFoodIfNotExists("CE_NO_FO_000003", "Mỳ Ý Tôm Sốt Kem",          50, 78000, "CE_NO_739204", 5,  8, 3, 8);

// ── Foods: Bánh ───────────────────────────────────
            createFoodIfNotExists("CE_CA_FO_000001", "Bánh Kem Socola",            30, 85000, "CE_CA_156893", 3,  6, 9, 3);
            createFoodIfNotExists("CE_CA_FO_000002", "Bánh Kem Dâu Tây",           30, 85000, "CE_CA_156893", 5,  5, 10, 5);
            createFoodIfNotExists("CE_CA_FO_000003", "Bánh Kem Muffin Vani",       30, 55000, "CE_CA_156893", 6,  3, 8, 4);

// ── Foods: Burger ─────────────────────────────────
            createFoodIfNotExists("CE_BU_FO_000001", "Burger Bò",                  50, 75000, "CE_BU_904561", 5, 6, 3, 3);
            createFoodIfNotExists("CE_BU_FO_000002", "Burger Gà",                  50, 70000, "CE_BU_904561", 4,  8, 5, 4);
            createFoodIfNotExists("CE_BU_FO_000003", "Burger Phô Mai",             50, 72000, "CE_BU_904561", 5, 4, 4, 5);

            // ── Kitchen Config ────────────────────────────────
            createConfigIfNotExists("MAX_TYPES_PER_DAY",    "10", "Tối đa số loại món Central Kitchen xử lý mỗi ngày");
            createConfigIfNotExists("MAX_QUANTITY_PER_DAY", "40", "Tối đa tổng số món Central Kitchen xử lý mỗi ngày");
        };




    }


    private void createOrderIfNotExists(
            String orderId,
            Integer priorityLevel,
            String note,
            String paymentOption,
            String paymentMethod,
            String paymentStatus,
            LocalDate orderDate,
            String statusOrder,
            String cancelReason,
            String storeId,
            LocalDateTime createdAt
    ) {
        if (orderRepository.existsById(orderId)) return;

        Order order = Order.builder()
                .orderId(orderId)
                .priorityLevel(priorityLevel)
                .note(note)
                .paymentOption(PaymentOption.valueOf(paymentOption))
                .paymentMethod(PaymentMethod.valueOf(paymentMethod))
                .paymentStatus(PaymentStatus.valueOf(paymentStatus))
                .orderDate(orderDate)
                .statusOrder(OrderStatus.valueOf(statusOrder))
                .cancelReason(cancelReason)
                .storeId(storeId)
                .createdAt(createdAt)
                .build();

        orderRepository.save(order);
    }

    private void createRoleIfNotExists(UserRole roleName) {
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .build()
                ));
    }

    private void createStoreIfNotExists(
            String storeId,
            String storeName,
            String address,
            String province,
            Integer ghnDistrictId,
            String ghnWardCode,
            String ward,
            String managerEmail
    ) {
        if (franchiseStoreRepository.existsById(storeId)) {
            return;
        }

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerEmail));

        // Build full address for GHN delivery
        String fullAddress = address + ", " + ward + ", " + province;

        FranchiseStore store = FranchiseStore.builder()
                .storeId(storeId)
                .storeName(storeName)
                .address(address)
                .province(province)
                .district(ghnDistrictId)          // ← Integer GHN district ID
                .ward(ghnWardCode)                 // ← String GHN ward code
                .address(fullAddress)          // ← combined for GHN API
                .deptStatus(false)
                .numberOfContact(manager.getPhone())
                .build();

        store.assignManager(manager);
        franchiseStoreRepository.save(store);
    }


    private void createUserIfNotExists(
            String email,
            String fullName,
            String phone,
            UserRole roleName
    ) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .phone(phone)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    private void createCategoryIfNotExists(String id, String name) {
        if (foodCategoryRepository.existsById(id)) return;

        CentralFoodCategory category = CentralFoodCategory.builder()
                .centralFoodTypeId(id)
                .centralFoodTypeName(name)
                .build();

        foodCategoryRepository.save(category);
    }

    private void createFoodIfNotExists(
            String foodId,
            String foodName,
            int amount,
            int unitPrice,
            String categoryId,
            int height,   // cm
            int length,   // cm
            int weight,   // gram
            int width     // cm
    ) {
        if (centralFoodRepository.existsById(foodId)) return;

        CentralFoods food = CentralFoods.builder()
                .centralFoodId(foodId)
                .foodName(foodName)
                .amount(new BigDecimal(amount))
                .unitPriceFood(unitPrice)
                .centralFoodStatus(FoodStatus.AVAILABLE)
                .manufacturingDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(30))
                .height(height)
                .length(length)
                .weight(weight)
                .width(width)
                .centralFoodTypeId(categoryId)
                .build();

        centralFoodRepository.save(food);
    }

    private void createConfigIfNotExists(String key, String value, String description) {
        if (kitchenConfigRepository.existsById(key)) return;

        kitchenConfigRepository.save(
                KitchenConfig.builder()
                        .configKey(key)
                        .configValue(value)
                        .description(description)
                        .build()
        );
    }
}