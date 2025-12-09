package com.warehouse.wms.dto;

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
public class StockCurrentDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private BigDecimal currentQuantity;
    private BigDecimal minimumStock;
    private LocalDateTime lastUpdated;
    private boolean lowStock;
}
