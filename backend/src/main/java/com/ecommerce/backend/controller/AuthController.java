package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.LoginRequest;
import com.ecommerce.backend.dto.request.RegisterRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Test endpoint — requires authentication
//    @GetMapping("/me")
//    public ResponseEntity<String> getCurrentUser(
//            @RequestHeader("Authorization") String authHeader) {
//        return ResponseEntity.ok("You are authenticated! Token: "
//                + authHeader.substring(0, 30) + "...");
//    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        return ResponseEntity.ok(authentication.getPrincipal());
    }
}