package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProfitLossStatementDTO;
import com.warehouse.wms.service.ProfitLossService;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports/profit-loss")
@RequiredArgsConstructor
@Slf4j
public class ProfitLossController {

    private final ProfitLossService profitLossService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String profitLossStatement(@RequestParam(required = false) Long warehouseId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            // Set default date range if not provided (current month)
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            // Generate P&L statement
            ProfitLossStatementDTO statement = profitLossService.generateStatement(
                    warehouseId, startDate, endDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());

            // Add data to model
            model.addAttribute("statement", statement);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/profit-loss/statement";
        } catch (Exception e) {
            log.error("Error generating P&L statement", e);
            model.addAttribute("error", "Error generating statement: " + e.getMessage());
            return "reports/profit-loss/statement";
        }
    }

    @GetMapping("/comparative")
    public String comparativeProfitLoss(@RequestParam(required = false) Long warehouseId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate1,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate1,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate2,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate2,
                                       Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            // Set default date ranges if not provided
            // Period 1: Current month
            if (startDate1 == null) {
                startDate1 = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate1 == null) {
                endDate1 = LocalDate.now();
            }

            // Period 2: Previous month
            if (startDate2 == null) {
                startDate2 = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            }
            if (endDate2 == null) {
                startDate2 = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                endDate2 = startDate2.withDayOfMonth(startDate2.lengthOfMonth());
            }

            // Generate comparative statements
            List<ProfitLossStatementDTO> statements = profitLossService.generateComparativeStatement(
                    warehouseId, startDate1, endDate1, startDate2, endDate2);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());

            // Add data to model
            model.addAttribute("statements", statements);
            model.addAttribute("statement1", statements.get(0));
            model.addAttribute("statement2", statements.get(1));
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("startDate1", startDate1);
            model.addAttribute("endDate1", endDate1);
            model.addAttribute("startDate2", startDate2);
            model.addAttribute("endDate2", endDate2);

            return "reports/profit-loss/comparative";
        } catch (Exception e) {
            log.error("Error generating comparative P&L statement", e);
            model.addAttribute("error", "Error generating statement: " + e.getMessage());
            return "reports/profit-loss/comparative";
        }
    }
}
