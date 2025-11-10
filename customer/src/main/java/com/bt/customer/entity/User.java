package com.bt.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "preferred_currency", length = 10)
    private String preferredCurrency;

    @Column(length = 500)
    private String address;

    @Column(length = 12)
    private String aadhaarNumber;

    @Column(length = 10)
    private String panNumber;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_classification", length = 20)
    private CustomerClassification classification;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (twoFactorEnabled == null) {
            twoFactorEnabled = false;
        }
        if (role == null) {
            role = Role.CUSTOMER;
        }
        if (preferredCurrency == null || preferredCurrency.isBlank()) {
            preferredCurrency = "KWD";
        }
        if (username != null) {
            username = username.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public enum Role {
        CUSTOMER,
        ADMIN,
        BANKOFFICER
    }

    public enum CustomerClassification {
        MINOR,
        REGULAR,
        SENIOR,
        VIP
    }

    public void setPreferredCurrency(String preferredCurrency) {
        this.preferredCurrency = preferredCurrency != null ? preferredCurrency.trim().toUpperCase() : null;
    }
}
