package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WishListController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getWishlist(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                wishlistService.getWishlist(user.getUsername()));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                wishlistService.toggleWishlist(productId, user.getUsername()));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Object>> check(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails user) {
        boolean wishlisted = wishlistService
                .isWishlisted(productId, user.getUsername());
        return ResponseEntity.ok(Map.of("wishlisted", wishlisted));
    }
}