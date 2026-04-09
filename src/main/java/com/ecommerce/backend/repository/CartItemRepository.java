package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Get all cart items for a user (with product eagerly loaded)
    @Query("SELECT ci FROM CartItem ci " +
            "JOIN FETCH ci.product p " +
            "JOIN FETCH p.category " +
            "WHERE ci.user.id = :userId")
    List<CartItem> findByUserIdWithProduct(@Param("userId") Long userId);

    // Find specific item (user + product combo)
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    // Count items in cart (for navbar badge)
    long countByUserId(Long userId);

    // Delete all items for a user (used after order placed)
    void deleteByUserId(Long userId);
}