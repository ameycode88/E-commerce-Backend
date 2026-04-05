package com.ecommerce.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private BigDecimal avgRating;
    private boolean active;
    private LocalDateTime createdAt;

    // Category info (flattened — no nested object)
    private Long categoryId;
    private String categoryName;

    // Seller info
    private Long sellerId;
    private String sellerName;
}