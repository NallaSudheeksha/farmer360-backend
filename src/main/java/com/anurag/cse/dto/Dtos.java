package com.anurag.cse.dto;

import com.anurag.cse.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

public class Dtos {

    private static void fail(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record RegisterRequest(String name, String phone, String password, Role role, String language, String address) {
        public void validate() {
            if (name == null || name.isBlank()) fail("Name is required");
            if (phone == null || !phone.matches("\\d{10}")) fail("Phone must be 10 digits");
            if (password == null || password.length() < 6) fail("Password must be at least 6 characters");
            if (role == null) fail("Role is required");
        }
    }

    public record LoginRequest(String phone, String password) {
        public void validate() {
            if (phone == null || phone.isBlank()) fail("Phone is required");
            if (password == null || password.isBlank()) fail("Password is required");
        }
    }

    public record AuthResponse(String token, String role, String name, String language) {}

    public record OrderRequest(Long cropId, double quantityKg, String deliveryAddress) {
        public void validate() {
            if (cropId == null) fail("Crop is required");
            if (quantityKg <= 0) fail("Quantity must be greater than 0");
            if (deliveryAddress == null || deliveryAddress.isBlank()) fail("Delivery address is required");
        }
    }

    public record UpdateCropRequest(double quantityKg, double pricePerKg) {
        public void validate() {
            if (quantityKg < 0) fail("Quantity cannot be negative");
            if (pricePerKg <= 0) fail("Price must be greater than 0");
        }
    }

    public record EventDto(String status, LocalDateTime time) {}

    public record CropDto(Long id, String name, double quantityKg, double pricePerKg,
                          String imageUrl, String pickupLocation, String farmerName) {
        public static CropDto from(Crop c) {
            return new CropDto(c.getId(), c.getName(), c.getQuantityKg(), c.getPricePerKg(),
                    c.getImageUrl(), c.getPickupLocation(), c.getFarmer().getName());
        }
    }

    public record OrderDto(Long id, String cropName, String imageUrl, double quantityKg, double totalAmount,
                           String status, String farmerName, String pickupLocation, String shopName,
                           String deliveryAddress, String transporterName, LocalDateTime createdAt) {
        public static OrderDto from(CropOrder o) {
            return new OrderDto(o.getId(), o.getCrop().getName(), o.getCrop().getImageUrl(), o.getQuantityKg(),
                    o.getTotalAmount(), o.getStatus().name(), o.getCrop().getFarmer().getName(),
                    o.getCrop().getPickupLocation(), o.getShopkeeper().getName(), o.getDeliveryAddress(),
                    o.getTransporter() == null ? null : o.getTransporter().getName(), o.getCreatedAt());
        }
    }
}