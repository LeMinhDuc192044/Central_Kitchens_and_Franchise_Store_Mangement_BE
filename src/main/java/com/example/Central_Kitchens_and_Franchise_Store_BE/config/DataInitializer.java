package com.example.Central_Kitchens_and_Franchise_Store_BE.config;


import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Role;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.User;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.UserRole;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.RoleRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.UserRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.TokenService;
import com.example.Central_Kitchens_and_Franchise_Store_BE.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Bean
    CommandLineRunner initTestUser() {
        return args -> {

            // Ensure STAFF role exists
            createRoleIfNotExists(UserRole.ADMIN);
            createRoleIfNotExists(UserRole.SUPPLY_COORDINATOR);
            createRoleIfNotExists(UserRole.CENTRAL_KITCHEN_STAFF);
            createRoleIfNotExists(UserRole.MANAGER);
            createRoleIfNotExists(UserRole.FRANCHISE_STAFF);


            createUserIfNotExists(
                    "admin@test.com",
                    "Admin Test User",
                    "0900000001",
                    UserRole.ADMIN
            );

            createUserIfNotExists(
                    "manager@test.com",
                    "Manager Test User",
                    "0900000001",
                    UserRole.MANAGER
            );

            createUserIfNotExists(
                        "staffFrachise@test.com",
                    "Franchise staff Test User",
                    "0900000002",
                    UserRole.FRANCHISE_STAFF
            );

            createUserIfNotExists(
                    "centralKitchenStaff@test.com",
                    "Central Kitchen Staff",
                    "09000000312",
                    UserRole.CENTRAL_KITCHEN_STAFF
            );

            createUserIfNotExists(
                    "supplyCoordinator@test.com",
                    "SUPPLY COORDINATOR",
                    "09000000321",
                    UserRole.SUPPLY_COORDINATOR
            );
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

    private void createUserIfNotExists(
            String email,
            String fullName,
            String phone,
            UserRole roleName
    ) {
        if (userRepository.existsByEmail(email)) return;

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

        String token = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        tokenService.saveUserToken(user, token, refreshToken);
    }
}




