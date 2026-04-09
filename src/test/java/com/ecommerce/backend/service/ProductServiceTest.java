package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository  productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository     userRepository;

    @InjectMocks ProductService productService;

    private User     seller;
    private Category category;
    private Product  product;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(1L).name("Test Seller").email("seller@test.com").build();

        category = Category.builder()
                .id(1L).name("Electronics").active(true).build();

        product = Product.builder()
                .id(1L).name("Test Phone").price(new BigDecimal("9999.00"))
                .stock(10).active(true).category(category).seller(seller)
                .avgRating(BigDecimal.ZERO).build();
    }

    @Test
    @DisplayName("getProductById returns correct DTO when product exists")
    void getProductById_WhenExists_ReturnsResponse() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Phone");
        assertThat(result.getPrice()).isEqualByComparingTo("9999.00");
        assertThat(result.getCategoryName()).isEqualTo("Electronics");
        assertThat(result.getSellerName()).isEqualTo("Test Seller");

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getProductById throws when product not found")
    void getProductById_WhenNotFound_ThrowsException() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createProduct saves product with correct seller and category")
    void createProduct_ValidRequest_SavesProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("New Laptop"); request.setPrice(new BigDecimal("45000"));
        request.setStock(5); request.setCategoryId(1L);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(userRepository.findByEmail("seller@test.com"))
                .thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0);
                    p.setId(2L);
                    return p;
                });

        ProductResponse result = productService
                .createProduct(request, "seller@test.com");

        assertThat(result.getName()).isEqualTo("New Laptop");
        assertThat(result.getSellerName()).isEqualTo("Test Seller");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct throws when seller doesn't own the product")
    void updateProduct_WrongSeller_ThrowsException() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductRequest request = new ProductRequest();
        request.setName("Updated"); request.setPrice(new BigDecimal("9000"));
        request.setStock(5); request.setCategoryId(1L);

        assertThatThrownBy(() ->
                productService.updateProduct(1L, request, "other@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("authorized");
    }

    @Test
    @DisplayName("deleteProduct performs soft delete, not hard delete")
    void deleteProduct_SetsActiveToFalse() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        productService.deleteProduct(1L, "seller@test.com");

        assertThat(product.isActive()).isFalse();
        verify(productRepository, times(1)).save(product);
        // Verify deleteById was NEVER called (it's a soft delete!)
        verify(productRepository, never()).deleteById(any());
    }
}
