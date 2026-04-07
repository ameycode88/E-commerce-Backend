package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.WishListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishListItem, Long> {
    List<WishListItem> findByUserId(Long userId);
    Optional<WishListItem> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
