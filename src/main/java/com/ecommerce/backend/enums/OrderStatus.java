package com.ecommerce.backend.enums;

public enum OrderStatus {
    PENDING,      // just placed, not yet confirmed
    CONFIRMED,    // payment received / confirmed by admin
    SHIPPED,      // dispatched by seller
    DELIVERED,    // received by customer
    CANCELLED     // cancelled before shipping
}