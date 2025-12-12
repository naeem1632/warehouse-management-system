package com.warehouse.wms.controller;

import com.warehouse.wms.dto.StockValuationDTO;
import com.warehouse.wms.dto.StockValuationSummaryDTO;
import com.warehouse.wms.service.StockValuationService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports/stock-valuation")
@RequiredArgsConstructor
@Slf4j
public class StockValuationController {

    private final StockValuationService stockValuationService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String stockValuation(@RequestParam(required = false) Long warehouseId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
                                Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            // Set default date if not provided (today)
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }

            // Get stock valuation
            StockValuationSummaryDTO summary = stockValuationService.getStockValuation(warehouseId, asOfDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());

            // Add data to model
            model.addAttribute("summary", summary);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("asOfDate", asOfDate);

            return "reports/stock-valuation/report";
        } catch (Exception e) {
            log.error("Error generating stock valuation report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/stock-valuation/report";
        }
    }

    @GetMapping("/by-warehouse")
    public String stockValuationByWarehouse(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
                                           Model model) {
        try {
            // Set default date (today)
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }

            // Get stock valuation by warehouse
            List<StockValuationSummaryDTO> summaries = stockValuationService.getStockValuationByWarehouse(asOfDate);

            model.addAttribute("summaries", summaries);
            model.addAttribute("asOfDate", asOfDate);

            return "reports/stock-valuation/by-warehouse";
        } catch (Exception e) {
            log.error("Error generating stock valuation by warehouse", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/stock-valuation/by-warehouse";
        }
    }

    @GetMapping("/high-value")
    public String highValueStock(@RequestParam(required = false) Long warehouseId,
                                 @RequestParam(defaultValue = "10000") BigDecimal threshold,
                                 Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            List<StockValuationDTO> highValueItems = stockValuationService.getHighValueStock(warehouseId, threshold);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());

            model.addAttribute("items", highValueItems);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("threshold", threshold);

            return "reports/stock-valuation/high-value";
        } catch (Exception e) {
            log.error("Error generating high value stock report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/stock-valuation/high-value";
        }
    }

    @GetMapping("/low-value")
    public String lowValueStock(@RequestParam(required = false) Long warehouseId,
                               @RequestParam(defaultValue = "1000") BigDecimal threshold,
                               Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            List<StockValuationDTO> lowValueItems = stockValuationService.getLowValueStock(warehouseId, threshold);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());

            model.addAttribute("items", lowValueItems);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("threshold", threshold);

            return "reports/stock-valuation/low-value";
        } catch (Exception e) {
            log.error("Error generating low value stock report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/stock-valuation/low-value";
        }
    }
}
