package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Create Razorpay order (called before showing payment popup)
    @PostMapping("/create/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> createPayment(
            @PathVariable Long orderId) throws RazorpayException {
        return ResponseEntity.ok(
                paymentService.createRazorpayOrder(orderId));
    }

    // Verify payment signature (called after Razorpay popup succeeds)
    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(
                paymentService.verifyPayment(
                        (String) body.get("razorpayOrderId"),
                        (String) body.get("razorpayPaymentId"),
                        (String) body.get("razorpaySignature"),
                        Long.valueOf(body.get("orderId").toString())
                )
        );
    }
}