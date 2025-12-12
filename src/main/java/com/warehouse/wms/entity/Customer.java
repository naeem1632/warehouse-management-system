package com.warehouse.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_code", columnList = "code"),
    @Index(name = "idx_customers_name", columnList = "name"),
    @Index(name = "idx_customers_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 50)
    private String designation;

    @Column(length = 20)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String ntn;

    @Column(length = 50)
    private String strn;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "opening_balance", precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_balance_type", length = 10)
    private String openingBalanceType; // DEBIT or CREDIT

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(length = 20)
    private String status; // ACTIVE, INACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
        if (openingBalance == null) {
            openingBalance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
