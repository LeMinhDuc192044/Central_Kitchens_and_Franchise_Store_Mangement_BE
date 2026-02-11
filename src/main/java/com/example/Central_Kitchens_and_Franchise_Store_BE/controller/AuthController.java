package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;


import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ApiResult;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AuthResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.AuthRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.RegisterRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Create new user", description = "create a new customer account")
    public ResponseEntity<ApiResult<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Create request received for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.<AuthResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("User created successfully")
                        .data(authService.register(request))
                        .build());
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResult<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request
    ) {
        log.info("Login request received for email: {}", request.getEmail());
        return ResponseEntity.ok(
                ApiResult.<AuthResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Login successful")
                        .data(authService.login(request))
                        .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResult<AuthResponse>> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken
    ) {
        log.info("Refresh token request received");
        return ResponseEntity.ok(
                ApiResult.<AuthResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Token refreshed successfully")
                        .data(authService.refreshToken(refreshToken))
                        .build());
    }


    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN', 'SUPPLY_COORDINATOR', 'CENTRAL_KITCHEN_STAFF')")
    @Operation(
            summary = "Logout user",
            description = "Logout current user and revoke all their tokens. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResult<Void>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.info("Logout request received for user: {}", email);
        authService.logout(email);

        return ResponseEntity.ok(
                ApiResult.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Logout successful")
                        .build());
    }

    @PostMapping("/logout-device")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FRANCHISE_STAFF', 'MANAGER', 'ADMIN', 'SUPPLY_COORDINATOR', 'CENTRAL_KITCHEN_STAFF')")
    @Operation(
            summary = "Logout from current device",
            description = "Logout from current device only. Other devices will remain logged in. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout from device successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<ApiResult<Void>> logoutFromDevice(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        log.info("Logout from device request received");
        authService.logoutFromDevice(token);

        return ResponseEntity.ok(
                ApiResult.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Logout from device successful")
                        .build());
    }
}

