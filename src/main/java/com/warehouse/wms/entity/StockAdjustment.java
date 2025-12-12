package com.warehouse.wms.entity;

import com.warehouse.wms.enums.AdjustmentStatus;
import com.warehouse.wms.enums.AdjustmentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments", indexes = {
    @Index(name = "idx_stock_adjustments_number", columnList = "adjustment_number"),
    @Index(name = "idx_stock_adjustments_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_stock_adjustments_product", columnList = "product_id"),
    @Index(name = "idx_stock_adjustments_date", columnList = "adjustment_date"),
    @Index(name = "idx_stock_adjustments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adjustment_number", nullable = false, unique = true, length = 50)
    private String adjustmentNumber;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private AdjustmentType adjustmentType;

    @Column(name = "quantity_before", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "adjustment_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal adjustmentQuantity;

    @Column(name = "quantity_after", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityAfter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AdjustmentStatus.PENDING;
        }
    }
}
