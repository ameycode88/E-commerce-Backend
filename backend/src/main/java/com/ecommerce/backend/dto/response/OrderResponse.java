package com.ecommerce.backend.dto.response;

import com.ecommerce.backend.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private String couponCode;
    private LocalDateTime createdAt;

    // Delivery address (snapshotted)
    private String deliveryName;
    private String deliveryPhone;
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPincode;

    // Order items
    private List<OrderItemResponse> items;

    @Data @Builder
    public static class OrderItemResponse {
        private Long id;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal itemTotal;
        private Long productId; // for "buy again" link
    }
}