package com.example.Central_Kitchens_and_Franchise_Store_BE.controller;



import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.ApiResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.AuthResponse;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.AuthRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.RegisterRequest;
import com.example.Central_Kitchens_and_Franchise_Store_BE.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Register request received for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AuthResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("User registered successfully")
                        .data(authService.register(request))
                        .build());
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request
    ) {
        log.info("Login request received for email: {}", request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Login successful")
                        .data(authService.login(request))
                        .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken
    ) {
        log.info("Refresh token request received");
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Token refreshed successfully")
                        .data(authService.refreshToken(refreshToken))
                        .build());
    }
}

