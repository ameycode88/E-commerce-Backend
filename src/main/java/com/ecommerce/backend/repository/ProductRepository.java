package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByActiveTrue();

    // ── Search by name (case-insensitive) ──
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(
            String name, Pageable pageable);

    // ── Filter by category ──
    Page<Product> findByCategoryIdAndActiveTrue(
            Long categoryId, Pageable pageable);

    // ── Search by name + filter by category ──
    Page<Product> findByNameContainingIgnoreCaseAndCategoryIdAndActiveTrue(
            String name, Long categoryId, Pageable pageable);

    // ── All active products ──
    Page<Product> findByActiveTrue(Pageable pageable);

    // ── All products by a specific seller ──
    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);

    // ── Custom JPQL query — search across name AND description ──
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(
            @Param("keyword") String keyword, Pageable pageable);

    // ── Products by category with keyword search ──
    @Query("SELECT p FROM Product p WHERE p.active = true " +
            "AND p.category.id = :categoryId " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}