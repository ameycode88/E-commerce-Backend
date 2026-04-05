package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // ───────────────────────────────────────────
    //  GET ALL — with search, filter, pagination
    // ───────────────────────────────────────────
    public Page<ProductResponse> getAllProducts(
            String keyword,
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategory = categoryId != null;

        if (hasKeyword && hasCategory) {
            products = productRepository
                    .searchByKeywordAndCategory(keyword, categoryId, pageable);
        } else if (hasKeyword) {
            products = productRepository
                    .searchByKeyword(keyword, pageable);
        } else if (hasCategory) {
            products = productRepository
                    .findByCategoryIdAndActiveTrue(categoryId, pageable);
        } else {
            products = productRepository.findByActiveTrue(pageable);
        }

        return products.map(this::toResponse);
    }

    // ───────────────────────────────────────────
    //  GET ONE
    // ───────────────────────────────────────────
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id));
        return toResponse(product);
    }

    // ───────────────────────────────────────────
    //  CREATE
    // ───────────────────────────────────────────
    public ProductResponse createProduct(ProductRequest request,
                                         String sellerEmail) {
        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() ->
                        new RuntimeException("Seller not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(category)
                .seller(seller)
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    // ───────────────────────────────────────────
    //  UPDATE
    // ───────────────────────────────────────────
    public ProductResponse updateProduct(Long id,
                                         ProductRequest request,
                                         String sellerEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        // Ensure seller owns this product
        if (!product.getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException(
                    "You are not authorized to update this product");
        }

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        return toResponse(productRepository.save(product));
    }

    // ───────────────────────────────────────────
    //  DELETE (soft delete — just mark inactive)
    // ───────────────────────────────────────────
    public void deleteProduct(Long id, String sellerEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        if (!product.getSeller().getEmail().equals(sellerEmail)) {
            throw new RuntimeException(
                    "You are not authorized to delete this product");
        }

        product.setActive(false);
        productRepository.save(product);
    }

    // ───────────────────────────────────────────
    //  UPDATE IMAGE URL (after Cloudinary upload)
    // ───────────────────────────────────────────
    public ProductResponse updateProductImage(Long id, String imageUrl) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));
        product.setImageUrl(imageUrl);
        return toResponse(productRepository.save(product));
    }

    // ───────────────────────────────────────────
    //  CONVERT entity → DTO
    // ───────────────────────────────────────────
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .imageUrl(p.getImageUrl())
                .avgRating(p.getAvgRating())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .sellerId(p.getSeller().getId())
                .sellerName(p.getSeller().getName())
                .build();
    }
}