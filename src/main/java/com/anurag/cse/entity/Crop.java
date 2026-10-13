package com.anurag.cse.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor
public class Crop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    private double quantityKg;
    private double pricePerKg;
    private String imageUrl;
    private String pickupLocation;
    @ManyToOne private User farmer;
    private LocalDateTime createdAt = LocalDateTime.now();
}