package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.PlaceOrderRequest;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderResponse.OrderItemResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.backend.exception.*;
import com.ecommerce.backend.service.CouponService;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository  cartItemRepository;
    private final ProductRepository   productRepository;
    private final UserRepository      userRepository;
    private final AddressRepository   addressRepository;


    private BigDecimal discount;
    private String couponCode;
    private final CouponService couponService;
    private final EmailService emailService;
    // ══════════════════════════════════════════════
    //  PLACE ORDER — the core business transaction
    //  @Transactional: ALL steps succeed or ALL fail
    // ══════════════════════════════════════════════
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request,
                                    String email) {

        // ── 1. Load user ──────────────────────────
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // ── 2. Load and validate address ──────────
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Address does not belong to user");
        }

        // ── 3. Load cart items ────────────────────
        List<CartItem> cartItems =
                cartItemRepository.findByUserIdWithProduct(user.getId());

        if (cartItems.isEmpty()) {
            throw new BadRequestException(
                    "Cart is empty. Add items before placing an order.");
        }

        // ── 4. Validate stock for ALL items first ─
        //      (fail fast before any DB changes)
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            if (!p.isActive()) {
                throw new BadRequestException(
                        "Product '" + p.getName() + "' is no longer available");
            }
            if (p.getStock() < ci.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for '" + p.getName() +
                                "'. Available: " + p.getStock() +
                                ", Requested: " + ci.getQuantity());
            }
        }
// / ///////////////////////////////////////////////////////////////////////////////////
        // ── 5. Calculate total ────────────────────
//        BigDecimal total = cartItems.stream()
//                .map(ci -> ci.getProduct().getPrice()
//                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // ── 6. Create the Order record ────────────
//        Order order = Order.builder()
//                .user(user)
//                // Snapshot address into order fields
//                .deliveryName(address.getFullName())
//                .deliveryPhone(address.getPhone())
//                .deliveryStreet(address.getStreet())
//                .deliveryCity(address.getCity())
//                .deliveryState(address.getState())
//                .deliveryPincode(address.getPincode())
//                .totalAmount(total)
//                .status(OrderStatus.PENDING)
//                .couponCode(request.getCouponCode())
//                .build();
//
//        Order savedOrder = orderRepository.save(order);
// ///////////////////////////////////////////////////////////////////////////////////////////////


        // ── 5. Calculate total ────────────────────
        BigDecimal total = cartItems.stream()
                .map(ci -> ci.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

// ── 5.1 Apply coupon (optional) ───────────
        BigDecimal discount = BigDecimal.ZERO;

        if (request.getCouponCode() != null
                && !request.getCouponCode().isEmpty()) {

            discount = couponService.validateAndApply(
                    request.getCouponCode(),
                    total
            );
        }

        BigDecimal finalTotal = total.subtract(discount);

// ── 6. Create the Order record ────────────
        Order order = Order.builder()
                .user(user)

                // snapshot address
                .deliveryName(address.getFullName())
                .deliveryPhone(address.getPhone())
                .deliveryStreet(address.getStreet())
                .deliveryCity(address.getCity())
                .deliveryState(address.getState())
                .deliveryPincode(address.getPincode())

                .totalAmount(finalTotal)   // ✅ use discounted total
                .discount(discount)        // ✅ save discount
                .couponCode(request.getCouponCode())

                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);



  //     /// /////////////////////////////////////////////////////////////////////////////

        // ── 7. Create OrderItems + decrement stock ─
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();

            // Create order item (snapshot product details)
            OrderItem item = OrderItem.builder()
                    .order(savedOrder)
                    .product(p)
                    .productName(p.getName())       // snapshot
                    .productImage(p.getImageUrl())  // snapshot
                    .price(p.getPrice())            // snapshot
                    .quantity(ci.getQuantity())
                    .itemTotal(p.getPrice()
                            .multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .build();

            orderItemRepository.save(item);

            // Decrement product stock
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
        }

        // ── 8. Clear the cart ──────────────────────
        cartItemRepository.deleteByUserId(user.getId());

        // ── 9. Return order details ────────────────
        return toResponse(savedOrder, cartItems);
    }

    // ══════════════════════════════════════════════
    //  GET SINGLE ORDER (by ID)
    // ══════════════════════════════════════════════
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String email) {
        Order order = orderRepository.findByIdWithItems(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        // Security: user can only view their own orders
        if (!order.getUser().getEmail().equals(email)) {
            throw new UnauthorizedException("Unauthorized to view this order");
        }
        return toResponse(order);
    }

    // ══════════════════════════════════════════════
    //  GET USER ORDER HISTORY
    // ══════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════
    //  ADMIN — GET ALL ORDERS (paginated)
    // ══════════════════════════════════════════════
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size,
                                            String status) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByStatus(
                    OrderStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::toResponse);
    }

    // ══════════════════════════════════════════════
    //  ADMIN — UPDATE ORDER STATUS
    // ══════════════════════════════════════════════
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId,
                                           String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId));

        OrderStatus nextStatus = OrderStatus.valueOf(
                newStatus.toUpperCase());

        // Validate status transition
        validateStatusTransition(order.getStatus(), nextStatus);

        // If cancelling after CONFIRMED, restore stock
        if (nextStatus == OrderStatus.CANCELLED
                && order.getStatus() == OrderStatus.CONFIRMED) {
            restoreStock(order);
        }

        order.setStatus(nextStatus);
        Order savedOrder = orderRepository.save(order);
        OrderResponse response = toResponse(savedOrder);
        
        if (nextStatus == OrderStatus.SHIPPED) {
            emailService.sendOrderShipped(order.getUser().getEmail(), response);
        } else if (nextStatus == OrderStatus.DELIVERED) {
            emailService.sendOrderDelivered(order.getUser().getEmail(), response);
        }

        return response;
    }

    // ══════════════════════════════════════════════
    //  HELPER — Validate allowed status transitions
    // ══════════════════════════════════════════════
    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING   -> next == OrderStatus.CONFIRMED
                    || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED
                    || next == OrderStatus.CANCELLED;
            case SHIPPED   -> next == OrderStatus.DELIVERED;
            case DELIVERED -> false; // terminal state
            case CANCELLED -> false; // terminal state
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " → " + next);
        }
    }

    // ══════════════════════════════════════════════
    //  HELPER — Restore stock on cancellation
    // ══════════════════════════════════════════════
    @Transactional
    private void restoreStock(Order order) {
        Order withItems = orderRepository.findByIdWithItems(order.getId());
        for (OrderItem item : withItems.getOrderItems()) {
            Product p = item.getProduct();
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }
    }

    // ══════════════════════════════════════════════
    //  CONVERTERS — entity → DTO
    // ══════════════════════════════════════════════
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = List.of();
        if (order.getOrderItems() != null) {
            items = order.getOrderItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }
        return buildResponse(order, items);
    }

    private OrderResponse toResponse(Order order,
                                     List<CartItem> cartItems) {
        List<OrderItemResponse> items = cartItems.stream()
                .map(ci -> OrderItemResponse.builder()
                        .productName(ci.getProduct().getName())
                        .productImage(ci.getProduct().getImageUrl())
                        .price(ci.getProduct().getPrice())
                        .quantity(ci.getQuantity())
                        .itemTotal(ci.getProduct().getPrice()
                                .multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .productId(ci.getProduct().getId())
                        .build())
                .collect(Collectors.toList());
        return buildResponse(order, items);
    }

    private OrderItemResponse toItemResponse(OrderItem oi) {
        return OrderItemResponse.builder()
                .id(oi.getId())
                .productName(oi.getProductName())
                .productImage(oi.getProductImage())
                .price(oi.getPrice())
                .quantity(oi.getQuantity())
                .itemTotal(oi.getItemTotal())
                .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                .build();
    }

    private OrderResponse buildResponse(Order o,
                                        List<OrderItemResponse> items) {
        return OrderResponse.builder()
                .id(o.getId())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .discount(o.getDiscount())
                .couponCode(o.getCouponCode())
                .createdAt(o.getCreatedAt())
                .deliveryName(o.getDeliveryName())
                .deliveryPhone(o.getDeliveryPhone())
                .deliveryStreet(o.getDeliveryStreet())
                .deliveryCity(o.getDeliveryCity())
                .deliveryState(o.getDeliveryState())
                .deliveryPincode(o.getDeliveryPincode())
                .items(items)
                .build();
    }
}