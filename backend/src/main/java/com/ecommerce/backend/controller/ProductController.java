package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.service.ImageUploadService;
import com.ecommerce.backend.service.ProductService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ImageUploadService imageUploadService;




    // ──────────────────────────────────────────
    // PUBLIC ENDPOINTS — no token needed
    // ──────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(
                productService.getAllProducts(
                        keyword, categoryId, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ──────────────────────────────────────────
    // SELLER/ADMIN ENDPOINTS — token required
    // ──────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(
                        request, currentUser.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return ResponseEntity.ok(
                productService.updateProduct(
                        id, request, currentUser.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        productService.deleteProduct(id, currentUser.getUsername());
        return ResponseEntity.ok("Product deleted successfully");
    }

    @PutMapping("/{id}/image")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateImage(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {

        return ResponseEntity.ok(
                productService.updateProductImage(id, body.get("imageUrl")));
    }




// And inject ImageUploadService in constructor via field

    @PostMapping("/{id}/upload-image")
//@PostMapping(value = "/{id}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")

    public ResponseEntity<ProductResponse> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        String imageUrl = imageUploadService.uploadImage(file, "products");
        return ResponseEntity.ok(
                productService.updateProductImage(id, imageUrl));
    }
}