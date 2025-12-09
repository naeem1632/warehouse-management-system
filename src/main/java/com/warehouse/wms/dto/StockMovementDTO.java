package com.warehouse.wms.dto;

import com.warehouse.wms.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private String productSku;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal rate;
    private String referenceType;
    private Long referenceId;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
