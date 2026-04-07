package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository   reviewRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;
    private final OrderRepository    orderRepository;

    // Add a review
    @Transactional
    public Map<String, Object> addReview(Long productId, Integer rating,
                                         String comment, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (reviewRepository.existsByUserIdAndProductId(
                user.getId(), productId)) {
            throw new RuntimeException(
                    "You have already reviewed this product");
        }

        Review review = Review.builder()
                .user(user).product(product)
                .rating(rating).comment(comment)
                .build();
        reviewRepository.save(review);

        // Recalculate and update product's average rating
        updateProductAvgRating(product);

        return Map.of(
                "message", "Review added successfully",
                "rating",  rating,
                "avgRating", product.getAvgRating()
        );
    }

    // Get all reviews for a product
    public List<Map<String, Object>> getProductReviews(Long productId) {
        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(r -> Map.<String, Object>of(
                        "id",          r.getId(),
                        "rating",      r.getRating(),
                        "comment",     r.getComment() != null ? r.getComment() : "",
                        "userName",    r.getUser().getName(),
                        "createdAt",   r.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    // Update product average rating after new review
    private void updateProductAvgRating(Product product) {
        Double avg = reviewRepository.calculateAvgRating(product.getId());
        if (avg != null) {
            product.setAvgRating(
                    BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            productRepository.save(product);
        }
    }
}
