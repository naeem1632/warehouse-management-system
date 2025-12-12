package com.warehouse.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_ledger", indexes = {
    @Index(name = "idx_customer_ledger_customer", columnList = "customer_id"),
    @Index(name = "idx_customer_ledger_date", columnList = "transaction_date"),
    @Index(name = "idx_customer_ledger_reference", columnList = "reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // opening_balance, sale, payment

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(precision = 15, scale = 2)
    private BigDecimal debit;

    @Column(precision = 15, scale = 2)
    private BigDecimal credit;

    @Column(precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "balance_type", length = 10)
    private String balanceType; // DEBIT or CREDIT

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (debit == null) {
            debit = BigDecimal.ZERO;
        }
        if (credit == null) {
            credit = BigDecimal.ZERO;
        }
    }
}
