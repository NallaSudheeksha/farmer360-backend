package com.anurag.cse.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** One row per status change: this is the tracking timeline of an order. */
@Entity @Table(name = "order_events") @Getter @Setter @NoArgsConstructor
public class OrderEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) private CropOrder order;
    @Enumerated(EnumType.STRING) private OrderStatus status;
    private LocalDateTime createdAt = LocalDateTime.now();
}