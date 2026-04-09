package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.entity.Address;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.AddressRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.exception.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<Address> getUserAddresses(String email) {
        User user = getUser(email);
        return addressRepository.findByUserId(user.getId());
    }

    @Transactional
    public Address addAddress(AddressRequest request, String email) {
        User user = getUser(email);

        // If this is the first address, make it default automatically
        boolean isFirst = addressRepository
                .findByUserId(user.getId()).isEmpty();

        Address address = Address.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .defaultAddress(isFirst)
                .user(user)
                .build();

        return addressRepository.save(address);
    }

    @Transactional
    public Address setDefaultAddress(Long addressId, String email) {
        User user = getUser(email);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address", addressId));

        // Security check — user can only set their own address as default
        if (!address.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        // Reset all other addresses to non-default first
        addressRepository.resetDefaultForUser(user.getId());

        // Set this one as default
        address.setDefaultAddress(true);
        return addressRepository.save(address);
    }

    public void deleteAddress(Long addressId, String email) {
        User user = getUser(email);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address", addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        addressRepository.delete(address);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + email));
    }
}