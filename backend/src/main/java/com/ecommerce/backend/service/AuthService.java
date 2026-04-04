package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.LoginRequest;
import com.ecommerce.backend.dto.request.RegisterRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.Role;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {

        // 1. Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email already registered: " + request.getEmail());
        }

        // 2. Build User object
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .phone(request.getPhone())
                .role(Role.CUSTOMER) // default role
                .active(true)
                .build();

        // 3. Save to DB
        User savedUser = userRepository.save(user);

        // 4. Generate JWT
        String token = jwtUtil.generateToken(savedUser);

        // 5. Return response
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .message("Registration successful!")
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        // 1. Authenticate — throws exception if wrong credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Load user from DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 3. Generate JWT
        String token = jwtUtil.generateToken(user);

        // 4. Return response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .message("Login successful!")
                .build();
    }
}