package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationDTO {
    private Long productId;
    private String productSku;
    private String productName;
    private String productUnit;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal currentQuantity;
    private BigDecimal averageCost;
    private BigDecimal totalValue;
    private BigDecimal lastPurchaseRate;
    private String lastPurchaseDate;
}
