package com.ecommerce.backend.service;

import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;
    private final ProductRepository productRepository;

    public Map<String, Object> getStats() {

        // Total revenue from delivered orders
        BigDecimal totalRevenue = orderRepository.findAll()
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders    = orderRepository.count();
        long pendingOrders  = orderRepository.countByStatus(OrderStatus.PENDING);
        long totalUsers     = userRepository.count();
        long totalProducts  = productRepository.countByActiveTrue();

        // Recent 5 orders
        var recentOrders = orderRepository
                .findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(o -> Map.of(
                        "id",           o.getId(),
                        "customerName", o.getDeliveryName(),
                        "total",        o.getTotalAmount(),
                        "status",       o.getStatus().name(),
                        "createdAt",    o.getCreatedAt().toString()
                ))
                .toList();

        return Map.of(
                "totalRevenue",   totalRevenue,
                "totalOrders",    totalOrders,
                "pendingOrders",  pendingOrders,
                "totalUsers",     totalUsers,
                "totalProducts",  totalProducts,
                "recentOrders",   recentOrders
        );
    }
}
