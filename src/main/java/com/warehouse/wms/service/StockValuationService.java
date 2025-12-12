package com.warehouse.wms.service;

import com.warehouse.wms.dto.StockValuationDTO;
import com.warehouse.wms.dto.StockValuationSummaryDTO;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.PurchaseItem;
import com.warehouse.wms.entity.StockCurrent;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.StockCurrentRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockValuationService {

    private final StockCurrentRepository stockCurrentRepository;
    private final PurchaseRepository purchaseRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public StockValuationSummaryDTO getStockValuation(Long warehouseId, LocalDate asOfDate) {
        log.info("Generating stock valuation for warehouse: {}, as of: {}", warehouseId, asOfDate);

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        // Get all current stock
        List<StockCurrent> currentStock = stockCurrentRepository.findAll();

        List<StockValuationDTO> valuationItems = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        int productCount = 0;
        String warehouseName = "All Warehouses";

        for (StockCurrent stock : currentStock) {
            // Filter by warehouse if specified
            if (warehouseId != null && !stock.getWarehouse().getId().equals(warehouseId)) {
                continue;
            }

            // Update warehouse name
            if (warehouseId != null) {
                warehouseName = stock.getWarehouse().getName();
            }

            // Only include products with positive stock
            if (stock.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
                productCount++;

                // Calculate average cost and last purchase info
                ValuationMetrics metrics = calculateValuationMetrics(
                        stock.getProduct().getId(),
                        stock.getWarehouse().getId(),
                        asOfDate);

                BigDecimal itemValue = stock.getCurrentQuantity().multiply(metrics.averageCost);

                StockValuationDTO dto = StockValuationDTO.builder()
                        .productId(stock.getProduct().getId())
                        .productSku(stock.getProduct().getSku())
                        .productName(stock.getProduct().getName())
                        .productUnit(stock.getProduct().getUnit().name())
                        .warehouseId(stock.getWarehouse().getId())
                        .warehouseName(stock.getWarehouse().getName())
                        .currentQuantity(stock.getCurrentQuantity())
                        .averageCost(metrics.averageCost)
                        .totalValue(itemValue)
                        .lastPurchaseRate(metrics.lastPurchaseRate)
                        .lastPurchaseDate(metrics.lastPurchaseDate)
                        .build();

                valuationItems.add(dto);
                totalQuantity = totalQuantity.add(stock.getCurrentQuantity());
                totalValue = totalValue.add(itemValue);
            }
        }

        return StockValuationSummaryDTO.builder()
                .valuationDate(asOfDate)
                .warehouseName(warehouseName)
                .totalProducts(productCount)
                .totalQuantity(totalQuantity)
                .totalValue(totalValue)
                .items(valuationItems)
                .build();
    }

    /**
     * Calculates valuation metrics for a product
     */
    private ValuationMetrics calculateValuationMetrics(Long productId, Long warehouseId, LocalDate asOfDate) {
        // Get all purchases for this product up to the specified date
        List<Purchase> purchases = purchaseRepository.findWithFilters(
                warehouseId, null, null, asOfDate.minusYears(2), asOfDate);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal lastPurchaseRate = BigDecimal.ZERO;
        String lastPurchaseDate = null;
        LocalDate latestDate = null;

        for (Purchase purchase : purchases) {
            if (purchase.getItems() != null) {
                for (PurchaseItem item : purchase.getItems()) {
                    if (item.getProduct().getId().equals(productId)) {
                        // Accumulate for weighted average
                        totalCost = totalCost.add(item.getAmount());
                        totalQuantity = totalQuantity.add(item.getQuantity());

                        // Track latest purchase
                        if (latestDate == null || purchase.getPurchaseDate().isAfter(latestDate)) {
                            latestDate = purchase.getPurchaseDate();
                            lastPurchaseRate = item.getRate();
                            lastPurchaseDate = latestDate.format(DATE_FORMATTER);
                        }
                    }
                }
            }
        }

        // Calculate weighted average cost
        BigDecimal averageCost;
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            averageCost = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
        } else {
            // No purchase history found, use zero
            averageCost = BigDecimal.ZERO;
            log.warn("No purchase history found for product: {} in warehouse: {}", productId, warehouseId);
        }

        return new ValuationMetrics(averageCost, lastPurchaseRate, lastPurchaseDate);
    }

    /**
     * Gets stock valuation by warehouse
     */
    @Transactional(readOnly = true)
    public List<StockValuationSummaryDTO> getStockValuationByWarehouse(LocalDate asOfDate) {
        List<StockCurrent> allStock = stockCurrentRepository.findAll();
        List<Long> warehouseIds = allStock.stream()
                .map(stock -> stock.getWarehouse().getId())
                .distinct()
                .toList();

        List<StockValuationSummaryDTO> summaries = new ArrayList<>();
        for (Long warehouseId : warehouseIds) {
            summaries.add(getStockValuation(warehouseId, asOfDate));
        }

        return summaries;
    }

    /**
     * Gets stock valuation for products with low value
     */
    @Transactional(readOnly = true)
    public List<StockValuationDTO> getLowValueStock(Long warehouseId, BigDecimal threshold) {
        StockValuationSummaryDTO summary = getStockValuation(warehouseId, LocalDate.now());

        return summary.getItems().stream()
                .filter(item -> item.getTotalValue().compareTo(threshold) < 0)
                .toList();
    }

    /**
     * Gets stock valuation for products with high value
     */
    @Transactional(readOnly = true)
    public List<StockValuationDTO> getHighValueStock(Long warehouseId, BigDecimal threshold) {
        StockValuationSummaryDTO summary = getStockValuation(warehouseId, LocalDate.now());

        return summary.getItems().stream()
                .filter(item -> item.getTotalValue().compareTo(threshold) > 0)
                .toList();
    }

    /**
     * Inner class to hold valuation metrics
     */
    @RequiredArgsConstructor
    @Getter
    private static class ValuationMetrics {
        private final BigDecimal averageCost;
        private final BigDecimal lastPurchaseRate;
        private final String lastPurchaseDate;
    }
}
