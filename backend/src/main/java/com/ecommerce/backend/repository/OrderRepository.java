package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.OrderStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Customer's order history — newest first (with items eagerly loaded)
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Admin — all orders paginated
    Page<Order> findAll(Pageable pageable);

    // Admin — filter by status
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Load order with all items in one query
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.id = :id")
    Order findByIdWithItems(@Param("id") Long id);
}