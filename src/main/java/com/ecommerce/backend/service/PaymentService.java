package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderResponse.OrderItemResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.entity.Payment.PaymentStatus;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.exception.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${razorpay.key.id}")     private String keyId;
    @Value("${razorpay.key.secret}") private String keySecret;

    // ─────────────────────────────────────────────
    //  STEP 1: Create Razorpay order
    //  Called when user clicks "Pay Now"
    // ─────────────────────────────────────────────
    public Map<String, Object> createRazorpayOrder(Long orderId)
            throws RazorpayException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId));

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        // Amount in paise (₹1 = 100 paise)
        int amountInPaise =
                order.getTotalAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .intValue();

        JSONObject options = new JSONObject();
        options.put("amount",   amountInPaise);
        options.put("currency", "INR");
        options.put("receipt",  "order_" + orderId);

        com.razorpay.Order razorpayOrder =
                client.orders.create(options);

        // Save payment record with CREATED status
        Payment payment = Payment.builder()
                .order(order)
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(order.getTotalAmount())
                .status(PaymentStatus.CREATED)
                .build();
        paymentRepository.save(payment);

        return Map.of(
                "razorpayOrderId", razorpayOrder.get("id"),
                "amount",          amountInPaise,
                "currency",        "INR",
                "keyId",           keyId,
                "orderId",         orderId
        );
    }

    // ─────────────────────────────────────────────
    //  STEP 2: Verify payment signature
    //  Called after Razorpay popup completes
    //  THIS IS THE SECURITY-CRITICAL STEP
    // ─────────────────────────────────────────────
    @Transactional
    public Map<String, Object> verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            Long   orderId) {

        // Generate expected signature using HMAC-SHA256
        String expectedSignature = generateHmacSHA256(
                razorpayOrderId + "|" + razorpayPaymentId,
                keySecret
        );

        // Compare with signature from Razorpay
        if (!expectedSignature.equals(razorpaySignature)) {
            // Signature mismatch — payment may be tampered!
            updatePaymentStatus(razorpayOrderId, PaymentStatus.FAILED);
            throw new PaymentException(
                    "Payment verification failed. Invalid signature.");
        }

        // Signature valid — mark payment as SUCCESS
        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment record not found: " + razorpayOrderId));

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order status to CONFIRMED
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Send order confirmation email
        User user = order.getUser();
        OrderResponse orderResponse = buildOrderResponse(order);
        emailService.sendOrderConfirmation(user.getEmail(), orderResponse);

        return Map.of(
                "message",   "Payment verified successfully",
                "orderId",   orderId,
                "status",    "SUCCESS"
        );
    }

    // ─────────────────────────────────────────────
    //  Build OrderResponse from Order entity
    // ─────────────────────────────────────────────
    private OrderResponse buildOrderResponse(Order order) {
        // Re-fetch order with items eagerly loaded to avoid LazyInitializationException
        Order fullOrder = orderRepository.findByIdWithItems(order.getId());

        List<OrderItemResponse> items = List.of();
        if (fullOrder.getOrderItems() != null) {
            items = fullOrder.getOrderItems().stream()
                    .map(oi -> OrderItemResponse.builder()
                            .id(oi.getId())
                            .productName(oi.getProductName())
                            .productImage(oi.getProductImage())
                            .price(oi.getPrice())
                            .quantity(oi.getQuantity())
                            .itemTotal(oi.getItemTotal())
                            .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
                .id(fullOrder.getId())
                .status(fullOrder.getStatus())
                .totalAmount(fullOrder.getTotalAmount())
                .discount(fullOrder.getDiscount())
                .couponCode(fullOrder.getCouponCode())
                .createdAt(fullOrder.getCreatedAt())
                .deliveryName(fullOrder.getDeliveryName())
                .deliveryPhone(fullOrder.getDeliveryPhone())
                .deliveryStreet(fullOrder.getDeliveryStreet())
                .deliveryCity(fullOrder.getDeliveryCity())
                .deliveryState(fullOrder.getDeliveryState())
                .deliveryPincode(fullOrder.getDeliveryPincode())
                .items(items)
                .build();
    }

    // ─────────────────────────────────────────────
    //  HMAC-SHA256 signature generation
    // ─────────────────────────────────────────────
    private String generateHmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new PaymentException("HMAC generation failed", e);
        }
    }

    private void updatePaymentStatus(String razorpayOrderId,
                                     PaymentStatus status) {
        paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .ifPresent(p -> {
                    p.setStatus(status);
                    paymentRepository.save(p);
                });
    }
}