package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.entity.Address;
import com.ecommerce.backend.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<Address>> getMyAddresses(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                addressService.getUserAddresses(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Address> addAddress(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                addressService.addAddress(request, user.getUsername()));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<Address> setDefault(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                addressService.setDefaultAddress(id, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        addressService.deleteAddress(id, user.getUsername());
        return ResponseEntity.ok(
                java.util.Map.of("message", "Address deleted"));
    }
}