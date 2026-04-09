package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Coupon;
import com.ecommerce.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ecommerce.backend.exception.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public BigDecimal validateAndApply(String code,
                                       BigDecimal orderTotal) {
        Coupon coupon = couponRepository
                .findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() ->
                        new BadRequestException("Invalid or expired coupon: " + code));

        // Check expiry
        if (coupon.getExpiryDate() != null
                && LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            throw new BadRequestException("Coupon has expired");
        }

        // Check minimum order
        if (coupon.getMinOrderAmount() != null
                && orderTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount for this coupon is ₹"
                    + coupon.getMinOrderAmount());
        }

        // Check usage limit
        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached");
        }

        // Calculate discount
        BigDecimal discount = orderTotal
                .multiply(coupon.getDiscountPercent())
                .divide(BigDecimal.valueOf(100));

        // Apply max discount cap if set
        if (coupon.getMaxDiscount() != null
                && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }

        // Increment usage count
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return discount;
    }

    // Admin: create coupon
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        return couponRepository.save(coupon);
    }
}