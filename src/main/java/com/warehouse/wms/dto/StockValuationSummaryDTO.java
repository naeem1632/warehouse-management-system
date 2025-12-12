package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationSummaryDTO {
    private LocalDate valuationDate;
    private String warehouseName;
    private Integer totalProducts;
    private BigDecimal totalQuantity;
    private BigDecimal totalValue;
    private List<StockValuationDTO> items;
}
