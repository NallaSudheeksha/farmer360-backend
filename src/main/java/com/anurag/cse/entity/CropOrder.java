package com.anurag.cse.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "orders") @Getter @Setter @NoArgsConstructor
public class CropOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne private Crop crop;
    @ManyToOne private User shopkeeper;
    @ManyToOne private User transporter;
    private double quantityKg;
    private double totalAmount;
    private String deliveryAddress;
    @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.PLACED;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime deliveredAt;
}