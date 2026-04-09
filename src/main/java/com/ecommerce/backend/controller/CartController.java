package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.CartRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")  // All cart endpoints require CUSTOMER role
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                cartService.getCart(user.getUsername()));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                cartService.addToCart(request, user.getUsername()));
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                cartService.updateQuantity(
                        cartItemId, body.get("quantity"), user.getUsername()));
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                cartService.removeFromCart(cartItemId, user.getUsername()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartCount(
            @AuthenticationPrincipal UserDetails user) {
        long count = cartService.getCartCount(user.getUsername());
        return ResponseEntity.ok(Map.of("count", count));
    }
}