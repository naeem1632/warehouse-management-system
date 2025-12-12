package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitLossStatementDTO {
    // Period information
    private LocalDate startDate;
    private LocalDate endDate;
    private String warehouseName;

    // Revenue section
    private BigDecimal grossSales;
    private BigDecimal salesReturns;
    private BigDecimal salesDiscounts;
    private BigDecimal netSales;

    // Cost of Goods Sold section
    private BigDecimal openingStock;
    private BigDecimal purchases;
    private BigDecimal purchaseReturns;
    private BigDecimal netPurchases;
    private BigDecimal closingStock;
    private BigDecimal costOfGoodsSold;

    // Gross Profit
    private BigDecimal grossProfit;
    private BigDecimal grossProfitMargin; // Percentage

    // Operating Expenses (placeholder for future expense tracking)
    private BigDecimal operatingExpenses;

    // Net Profit
    private BigDecimal netProfit;
    private BigDecimal netProfitMargin; // Percentage

    // Additional metrics
    private Integer totalSalesCount;
    private Integer totalPurchasesCount;
    private BigDecimal averageSaleValue;
    private BigDecimal averagePurchaseValue;
}
