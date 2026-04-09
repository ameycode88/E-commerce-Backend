package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.PlaceOrderRequest;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository     orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CartItemRepository  cartItemRepository;
    @Mock ProductRepository   productRepository;
    @Mock UserRepository      userRepository;
    @Mock AddressRepository   addressRepository;

    @InjectMocks OrderService orderService;

    private User    user;
    private Address address;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("customer@test.com")
                .name("Test Customer").build();

        address = Address.builder().id(1L).fullName("Test Customer")
                .phone("9876543210").street("42, Koramangala")
                .city("Bengaluru").state("Karnataka").pincode("560034")
                .user(user).build();

        Category cat = Category.builder().id(1L).name("Electronics").build();
        product = Product.builder().id(1L).name("Test Phone")
                .price(new BigDecimal("9999.00")).stock(10)
                .active(true).category(cat)
                .avgRating(BigDecimal.ZERO).build();

        cartItem = CartItem.builder().id(1L)
                .user(user).product(product).quantity(2).build();
    }

    @Test
    @DisplayName("placeOrder creates order, decrements stock, clears cart")
    void placeOrder_ValidCart_CreatesOrderAndDecrementsStock() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(user));
        when(addressRepository.findById(1L))
                .thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdWithProduct(1L))
                .thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(1L);
                    return o;
                });
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrderResponse result = orderService
                .placeOrder(request, "customer@test.com");

        assertThat(result).isNotNull();
        // Total: 9999 × 2 = 19998
        assertThat(result.getTotalAmount())
                .isEqualByComparingTo("19998.00");
        // Stock should be decremented: 10 - 2 = 8
        assertThat(product.getStock()).isEqualTo(8);
        // Cart should be cleared
        verify(cartItemRepository, times(1))
                .deleteByUserId(1L);
    }

    @Test
    @DisplayName("placeOrder throws when cart is empty")
    void placeOrder_EmptyCart_ThrowsException() {
        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(user));
        when(addressRepository.findById(1L))
                .thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdWithProduct(1L))
                .thenReturn(Collections.emptyList());

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);

        assertThatThrownBy(() ->
                orderService.placeOrder(request, "customer@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("placeOrder throws when product stock is insufficient")
    void placeOrder_InsufficientStock_ThrowsException() {
        product.setStock(1); // only 1 in stock
        cartItem = CartItem.builder().id(1L)
                .user(user).product(product).quantity(5).build(); // want 5

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(user));
        when(addressRepository.findById(1L))
                .thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdWithProduct(1L))
                .thenReturn(List.of(cartItem));

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);

        assertThatThrownBy(() ->
                orderService.placeOrder(request, "customer@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("stock");

        // Verify no order was saved
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrderStatus PENDING to CONFIRMED succeeds")
    void updateStatus_ValidTransition_UpdatesStatus() {
        Order order = Order.builder().id(1L)
                .status(OrderStatus.PENDING).user(user).build();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderResponse result = orderService
                .updateOrderStatus(1L, "CONFIRMED");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateOrderStatus throws on invalid transition DELIVERED to PENDING")
    void updateStatus_InvalidTransition_ThrowsException() {
        Order order = Order.builder().id(1L)
                .status(OrderStatus.DELIVERED).user(user).build();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(1L, "PENDING"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status transition");
    }
}