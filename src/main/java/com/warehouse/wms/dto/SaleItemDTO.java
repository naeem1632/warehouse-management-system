package com.warehouse.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemDTO {
    private Long id;
    private Long saleId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private BigDecimal quantity;
    private BigDecimal rate;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal amount;

    // Additional field for form - available stock
    private BigDecimal availableStock;
}
