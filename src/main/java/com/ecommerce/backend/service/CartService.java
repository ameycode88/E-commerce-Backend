package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.CartRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.dto.response.CartResponse.CartItemResponse;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.exception.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────
    //  GET CART
    // ─────────────────────────────────────
    public CartResponse getCart(String email) {
        User user = getUser(email);

        List<CartItem> items =
                cartItemRepository.findByUserIdWithProduct(user.getId());

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalPrice(total)
                .build();
    }

    // ─────────────────────────────────────
    //  ADD TO CART
    // ─────────────────────────────────────
    @Transactional
    public CartResponse addToCart(CartRequest request, String email) {
        User user = getUser(email);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException(
                    "Only " + product.getStock() + " items in stock");
        }

        // Check if product already in cart
        Optional<CartItem> existing = cartItemRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existing.isPresent()) {
            // Update quantity instead of creating a new row
            CartItem item = existing.get();
            int newQty = item.getQuantity() + request.getQuantity();

            if (newQty > product.getStock()) {
                throw new BadRequestException(
                        "Cannot add more. Only " + product.getStock()
                                + " in stock, you already have "
                                + item.getQuantity() + " in cart.");
            }

            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            // Create new cart item
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCart(email);
    }

    // ─────────────────────────────────────
    //  UPDATE QUANTITY
    // ─────────────────────────────────────
    @Transactional
    public CartResponse updateQuantity(Long cartItemId,
                                       int newQuantity,
                                       String email) {
        User user = getUser(email);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item", cartItemId));

        // Security check
        if (!item.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        if (newQuantity <= 0) {
            // If qty becomes 0 or negative, remove the item
            cartItemRepository.delete(item);
        } else {
            if (newQuantity > item.getProduct().getStock()) {
                throw new BadRequestException(
                        "Only " + item.getProduct().getStock()
                                + " items available");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        }

        return getCart(email);
    }

    // ─────────────────────────────────────
    //  REMOVE FROM CART
    // ─────────────────────────────────────
    @Transactional
    public CartResponse removeFromCart(Long cartItemId, String email) {
        User user = getUser(email);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item", cartItemId));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        cartItemRepository.delete(item);
        return getCart(email);
    }

    // ─────────────────────────────────────
    //  CLEAR CART (called after order placed)
    // ─────────────────────────────────────
    @Transactional
    public void clearCart(String email) {
        User user = getUser(email);
        cartItemRepository.deleteByUserId(user.getId());
    }

    // ─────────────────────────────────────
    //  CART COUNT (for navbar badge)
    // ─────────────────────────────────────
    public long getCartCount(String email) {
        User user = getUser(email);
        return cartItemRepository.countByUserId(user.getId());
    }

    // ─────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + email));
    }

    private CartItemResponse toItemResponse(CartItem ci) {
        BigDecimal itemTotal = ci.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(ci.getQuantity()));

        return CartItemResponse.builder()
                .cartItemId(ci.getId())
                .productId(ci.getProduct().getId())
                .productName(ci.getProduct().getName())
                .productImage(ci.getProduct().getImageUrl())
                .productPrice(ci.getProduct().getPrice())
                .quantity(ci.getQuantity())
                .itemTotal(itemTotal)
                .availableStock(ci.getProduct().getStock())
                .categoryName(ci.getProduct().getCategory().getName())
                .build();
    }
}