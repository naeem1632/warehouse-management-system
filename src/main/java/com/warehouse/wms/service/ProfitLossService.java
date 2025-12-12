package com.warehouse.wms.service;

import com.warehouse.wms.dto.ProfitLossStatementDTO;
import com.warehouse.wms.entity.*;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.SaleRepository;
import com.warehouse.wms.repository.StockCurrentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitLossService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final StockCurrentRepository stockCurrentRepository;

    @Transactional(readOnly = true)
    public ProfitLossStatementDTO generateStatement(Long warehouseId,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        log.info("Generating P&L statement for warehouse: {}, period: {} to {}",
                warehouseId, startDate, endDate);

        // Get sales data
        List<Sale> sales = saleRepository.findWithFilters(warehouseId, null, startDate, endDate);

        // Get purchases data
        List<Purchase> purchases = purchaseRepository.findWithFilters(
                warehouseId, null, null, startDate, endDate);

        // Calculate revenue metrics
        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal salesDiscounts = BigDecimal.ZERO;
        BigDecimal salesReturns = BigDecimal.ZERO; // Placeholder for future returns module

        for (Sale sale : sales) {
            grossSales = grossSales.add(sale.getTotalAmount());
            if (sale.getDiscount() != null) {
                salesDiscounts = salesDiscounts.add(sale.getDiscount());
            }
        }

        BigDecimal netSales = grossSales.subtract(salesDiscounts).subtract(salesReturns);

        // Calculate purchase metrics
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal purchaseReturns = BigDecimal.ZERO; // Placeholder for future returns module

        for (Purchase purchase : purchases) {
            totalPurchases = totalPurchases.add(purchase.getTotalAmount());
        }

        BigDecimal netPurchases = totalPurchases.subtract(purchaseReturns);

        // Estimate stock valuation
        // Note: This is a simplified calculation. For accurate COGS, we would need:
        // 1. Opening stock value at start date
        // 2. Closing stock value at end date
        // 3. Actual cost tracking per sale item
        // For now, we'll use a simplified approach
        BigDecimal openingStock = estimateStockValue(warehouseId, startDate);
        BigDecimal closingStock = estimateStockValue(warehouseId, endDate);

        // Calculate Cost of Goods Sold
        // COGS = Opening Stock + Net Purchases - Closing Stock
        BigDecimal costOfGoodsSold = openingStock.add(netPurchases).subtract(closingStock);

        // Ensure COGS is not negative
        if (costOfGoodsSold.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Negative COGS calculated: {}. Setting to net purchases.", costOfGoodsSold);
            costOfGoodsSold = netPurchases;
        }

        // Calculate Gross Profit
        BigDecimal grossProfit = netSales.subtract(costOfGoodsSold);
        BigDecimal grossProfitMargin = calculateMargin(grossProfit, netSales);

        // Operating expenses (placeholder for future expense tracking module)
        BigDecimal operatingExpenses = BigDecimal.ZERO;

        // Calculate Net Profit
        BigDecimal netProfit = grossProfit.subtract(operatingExpenses);
        BigDecimal netProfitMargin = calculateMargin(netProfit, netSales);

        // Calculate additional metrics
        BigDecimal averageSaleValue = sales.isEmpty() ? BigDecimal.ZERO :
                netSales.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        BigDecimal averagePurchaseValue = purchases.isEmpty() ? BigDecimal.ZERO :
                netPurchases.divide(BigDecimal.valueOf(purchases.size()), 2, RoundingMode.HALF_UP);

        // Get warehouse name if specific warehouse requested
        String warehouseName = warehouseId != null ? getWarehouseName(warehouseId, sales, purchases) : "All Warehouses";

        return ProfitLossStatementDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .warehouseName(warehouseName)
                .grossSales(grossSales)
                .salesReturns(salesReturns)
                .salesDiscounts(salesDiscounts)
                .netSales(netSales)
                .openingStock(openingStock)
                .purchases(totalPurchases)
                .purchaseReturns(purchaseReturns)
                .netPurchases(netPurchases)
                .closingStock(closingStock)
                .costOfGoodsSold(costOfGoodsSold)
                .grossProfit(grossProfit)
                .grossProfitMargin(grossProfitMargin)
                .operatingExpenses(operatingExpenses)
                .netProfit(netProfit)
                .netProfitMargin(netProfitMargin)
                .totalSalesCount(sales.size())
                .totalPurchasesCount(purchases.size())
                .averageSaleValue(averageSaleValue)
                .averagePurchaseValue(averagePurchaseValue)
                .build();
    }

    /**
     * Estimates stock value using weighted average cost from recent purchases
     * This is a simplified approach. For production, consider implementing
     * proper stock valuation with FIFO, LIFO, or weighted average cost tracking.
     */
    private BigDecimal estimateStockValue(Long warehouseId, LocalDate asOfDate) {
        // Get current stock quantities
        // Note: This gets CURRENT stock, not historical stock at asOfDate
        // For accurate historical valuation, we would need stock_history table
        List<StockCurrent> currentStock = stockCurrentRepository.findAll();

        // For each product, estimate value using recent purchase prices
        BigDecimal totalValue = BigDecimal.ZERO;

        for (StockCurrent stock : currentStock) {
            // Filter by warehouse if specified
            if (warehouseId != null && !stock.getWarehouse().getId().equals(warehouseId)) {
                continue;
            }

            if (stock.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
                // Get recent purchase price for this product
                BigDecimal estimatedCost = getEstimatedCostPrice(
                        stock.getProduct().getId(),
                        stock.getWarehouse().getId(),
                        asOfDate);

                BigDecimal stockValue = stock.getCurrentQuantity().multiply(estimatedCost);
                totalValue = totalValue.add(stockValue);
            }
        }

        log.debug("Estimated stock value as of {}: {}", asOfDate, totalValue);
        return totalValue;
    }

    /**
     * Gets estimated cost price for a product based on recent purchases
     */
    private BigDecimal getEstimatedCostPrice(Long productId, Long warehouseId, LocalDate asOfDate) {
        // Get recent purchases for this product before the specified date
        List<Purchase> recentPurchases = purchaseRepository.findWithFilters(
                warehouseId, null, null, asOfDate.minusMonths(6), asOfDate);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (Purchase purchase : recentPurchases) {
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProduct().getId().equals(productId)) {
                        totalCost = totalCost.add(item.getAmount());
                        totalQuantity = totalQuantity.add(item.getQuantity());
                    }
                }
            }
        }

        // Calculate weighted average cost
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
        }

        // Default to zero if no recent purchases found
        return BigDecimal.ZERO;
    }

    /**
     * Calculates profit margin as a percentage
     */
    private BigDecimal calculateMargin(BigDecimal profit, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return profit.divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets warehouse name from sales or purchases
     */
    private String getWarehouseName(Long warehouseId, List<Sale> sales, List<Purchase> purchases) {
        if (!sales.isEmpty()) {
            return sales.get(0).getWarehouse().getName();
        }
        if (!purchases.isEmpty()) {
            return purchases.get(0).getWarehouse().getName();
        }
        return "Unknown Warehouse";
    }

    /**
     * Generates comparative P&L statements for multiple periods
     */
    @Transactional(readOnly = true)
    public List<ProfitLossStatementDTO> generateComparativeStatement(
            Long warehouseId,
            LocalDate startDate1, LocalDate endDate1,
            LocalDate startDate2, LocalDate endDate2) {

        ProfitLossStatementDTO period1 = generateStatement(warehouseId, startDate1, endDate1);
        ProfitLossStatementDTO period2 = generateStatement(warehouseId, startDate2, endDate2);

        return List.of(period1, period2);
    }
}
