package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "Address ID is required")
    private Long addressId;

    private String couponCode; // optional
}
