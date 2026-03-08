package com.example.Central_Kitchens_and_Franchise_Store_BE.config;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStore;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStorePaymentMethod;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Role;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.User;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.UserRole;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStorePaymentMethodRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStoreRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.RoleRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FranchiseStoreRepository franchiseStoreRepository;
    private final FranchiseStorePaymentMethodRepository paymentMethodRepository;

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
                    "123 Nguyen Hue St, District 1",
                    "District 1",
                    "Ben Nghe Ward",
                    "store1.manager@centralkitchen.com"
            );
            createPaymentMethodIfNotExists("STORE-D1-001", "CREDIT");

            // ── Store 2 - District 2 ──────────────────────────────
            createStoreIfNotExists(
                    "STORE-D2-001",
                    "Central Kitchen District 2",
                    "456 Tran Nao St, District 2",
                    "District 2",
                    "An Khanh Ward",
                    "store2.manager@centralkitchen.com"
            );
            createPaymentMethodIfNotExists("STORE-D2-001", "CREDIT");

            // ── Store 3 - District 3 ──────────────────────────────
            createStoreIfNotExists(
                    "STORE-D3-001",
                    "Central Kitchen District 3",
                    "789 Vo Van Tan St, District 3",
                    "District 3",
                    "Ward 6",
                    "store3.manager@centralkitchen.com"
            );
            createPaymentMethodIfNotExists("STORE-D3-001", "CREDIT");
        };
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
            String district,
            String ward,
            String managerEmail
    ) {
        if (franchiseStoreRepository.existsById(storeId)) {
            return;
        }

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerEmail));

        FranchiseStore store = FranchiseStore.builder()
                .storeId(storeId)
                .storeName(storeName)
                .address(address)
                .district(district)
                .ward(ward)
                .revenue(10000000)
                .deptStatus(false)
                .numberOfContact(manager.getPhone())
                .build();

        store.assignManager(manager);
        franchiseStoreRepository.save(store);
    }

    private void createPaymentMethodIfNotExists(String storeId, String method) {
        boolean exists = paymentMethodRepository
                .findByStoreIdAndPaymentMethod(storeId, method)
                .isPresent();
        if (!exists) {
            FranchiseStorePaymentMethod pm = new FranchiseStorePaymentMethod();
            pm.setStorePaymentId(UUID.randomUUID().toString());
            pm.setStoreId(storeId);
            pm.setPaymentMethod(method);
            paymentMethodRepository.save(pm);
        }
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
}