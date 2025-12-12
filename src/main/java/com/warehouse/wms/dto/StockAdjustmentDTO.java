package com.warehouse.wms.dto;

import com.warehouse.wms.enums.AdjustmentStatus;
import com.warehouse.wms.enums.AdjustmentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentDTO {
    private Long id;
    private String adjustmentNumber;
    private LocalDate adjustmentDate;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private AdjustmentType adjustmentType;
    private BigDecimal quantityBefore;
    private BigDecimal adjustmentQuantity;
    private BigDecimal quantityAfter;
    private String reason;
    private AdjustmentStatus status;
    private Long createdBy;
    private String createdByName;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
