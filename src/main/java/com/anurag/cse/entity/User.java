package com.anurag.cse.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true, nullable = false) private String phone;
    private String password;
    @Enumerated(EnumType.STRING) private Role role;
    private String language = "en";
    private String address;
}