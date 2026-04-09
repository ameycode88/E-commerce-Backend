package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;                    // e.g. "SAVE20"

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;     // e.g. 20.00 = 20% off

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;      // minimum cart total

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;         // cap on discount amount

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;             // null = unlimited

    @Column(name = "used_count")
    private Integer usedCount = 0;

    private boolean active = true;
}
