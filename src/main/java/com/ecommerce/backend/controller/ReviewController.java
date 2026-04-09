package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // PUBLIC — anyone can read reviews
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getReviews(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                reviewService.getProductReviews(productId));
    }

    // CUSTOMER only — add a review
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> addReview(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {

        Integer rating  = (Integer) body.get("rating");
        String  comment = (String)  body.get("comment");

        return ResponseEntity.ok(
                reviewService.addReview(
                        productId, rating, comment, user.getUsername()));
    }
}