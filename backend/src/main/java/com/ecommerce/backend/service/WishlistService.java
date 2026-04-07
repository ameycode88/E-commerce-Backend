package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

        private final WishListRepository wishlistRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;

        // Toggle: add if not present, remove if already present
        @Transactional
        public Map<String, Object> toggleWishlist(Long productId, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                boolean exists = wishlistRepository
                                .existsByUserIdAndProductId(user.getId(), productId);

                if (exists) {
                        wishlistRepository
                                        .deleteByUserIdAndProductId(user.getId(), productId);
                        return Map.of("wishlisted", false,
                                        "message", "Removed from wishlist");
                } else {
                        Product product = productRepository.findById(productId)
                                        .orElseThrow(() -> new RuntimeException("Product not found"));
                        WishListItem item = WishListItem.builder()
                                        .user(user).product(product).build();
                        wishlistRepository.save(item);
                        return Map.of("wishlisted", true,
                                        "message", "Added to wishlist");
                }
        }

        // Get full wishlist
        @Transactional(readOnly = true)
        public List<Map<String, Object>> getWishlist(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return wishlistRepository.findByUserId(user.getId())
                                .stream()
                                .map(item -> {
                                        Product p = item.getProduct();
                                        return Map.<String, Object>of(
                                                        "wishlistItemId", item.getId(),
                                                        "productId", p.getId(),
                                                        "productName", p.getName(),
                                                        "productImage", p.getImageUrl() != null ? p.getImageUrl() : "",
                                                        "price", p.getPrice(),
                                                        "avgRating", p.getAvgRating() != null ? p.getAvgRating() : java.math.BigDecimal.ZERO,
                                                        "inStock", p.getStock() > 0,
                                                        "categoryName",
                                                        p.getCategory() != null ? p.getCategory().getName() : "");
                                })
                                .collect(Collectors.toList());
        }

        // Check if a product is wishlisted by user
        @Transactional(readOnly = true)
        public boolean isWishlisted(Long productId, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                return wishlistRepository
                                .existsByUserIdAndProductId(user.getId(), productId);
        }
}