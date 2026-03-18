package com.example.Central_Kitchens_and_Franchise_Store_BE.service;

import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AuthResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.FranchiseStoreInfo;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.AuthRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.RegisterRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.FranchiseStore;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.Role;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.User;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.UserRole;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.AuthenticationException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.FranchiseStoreRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.RoleRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.UserRepository;
import com.example.Central_Kitchens_and_Franchise_Store_BE.util.JwtUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FranchiseStoreRepository franchiseStoreRepository;  // ✅ Add this
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Get role from database
        Role role = roleRepository.findById(request.getIdRole())
                .orElseThrow(() -> new RuntimeException("Role not found " + request.getIdRole()));

//        UserRole userRole = UserRole.valueOf(request.getRoleName());
//        Role role = roleRepository.findByName(userRole)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));
        FranchiseStoreInfo franchiseStoreInfo = null;
        FranchiseStore store = null;
        if (role.getName() == UserRole.FRANCHISE_STAFF) {
            if (request.getIdRole() == null || request.getStoreId().isEmpty()) {
                throw new AuthenticationException("Store ID is required for Franchise Staff registration");
            }

            // Check if store exists
            store = franchiseStoreRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise store not found: " + request.getStoreId()));


            // Check if store already has a manager
            if (store.getManager() != null) {
                throw new AuthenticationException("This store already has a manager: " + store.getManager().getFullName());
            }

        }



        // Create new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);

        if (store != null) {
            store.assignManager(user);
            store.setNumberOfContact(user.getPhone());
            franchiseStoreRepository.save(store);
            franchiseStoreInfo = toInfo(store);
        }


        String jwtToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        tokenService.revokeAllUserTokens(user);
        tokenService.saveUserToken(user, jwtToken, refreshToken);

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName().name())
                .franchiseStoreInfo(franchiseStoreInfo)   // ✅ NOW IT WORKS
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        FranchiseStoreInfo franchiseStoreInfo = null;
        if (user.getRole().getName().equals(UserRole.FRANCHISE_STAFF))
        {
            franchiseStoreInfo = toInfo(user.getManagedStore());
        }


        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtils.generateToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        tokenService.revokeAllUserTokens(user);
        tokenService.saveUserToken(user, token, refreshToken);

        log.info("User logged in successfully: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName().name())
                .userId(user.getId())
                .franchiseStoreInfo(franchiseStoreInfo)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        String username = jwtUtils.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtils.isTokenValid(refreshToken, userDetails)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String newToken = jwtUtils.generateToken(userDetails);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName().name())
                .userId(user.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public static FranchiseStoreInfo toInfo(FranchiseStore store) {
        if (store == null) {
            return null;
        }

        return FranchiseStoreInfo.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .deptStatus(store.isDeptStatus())
                .district(store.getDistrict())
                .ward(store.getWard())
                .numberOfContact(store.getNumberOfContact())
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        tokenService.revokeAllUserTokens(user);
    }

    @Transactional
    public void logoutFromDevice(String token) {
        tokenService.revokeToken(token);
    }


    @Transactional
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getName().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .franchiseStoreInfo(toInfo(user.getManagedStore()))
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private String id;
        private String fullName;
        private String email;
        private String phone;
        private String role;        // e.g. "FRANCHISE_STAFF", "ADMIN"
        private Boolean active;
        private FranchiseStoreInfo franchiseStoreInfo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
