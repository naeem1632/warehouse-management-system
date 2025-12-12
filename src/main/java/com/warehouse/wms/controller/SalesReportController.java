package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ReportSummaryDTO;
import com.warehouse.wms.dto.SalesReportDTO;
import com.warehouse.wms.service.CustomerService;
import com.warehouse.wms.service.SalesReportService;
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
@RequestMapping("/reports/sales")
@RequiredArgsConstructor
@Slf4j
public class SalesReportController {

    private final SalesReportService salesReportService;
    private final WarehouseService warehouseService;
    private final CustomerService customerService;

    @GetMapping
    public String salesReport(@RequestParam(required = false) Long warehouseId,
                             @RequestParam(required = false) Long customerId,
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

            // Get report data
            List<SalesReportDTO> sales = salesReportService.getSalesReport(
                    warehouseId, customerId, startDate, endDate);

            ReportSummaryDTO summary = salesReportService.getSalesSummary(
                    warehouseId, customerId, startDate, endDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());
            model.addAttribute("customers", customerService.getAllCustomers());

            // Add data to model
            model.addAttribute("sales", sales);
            model.addAttribute("summary", summary);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("selectedCustomerId", customerId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/sales/list";
        } catch (Exception e) {
            log.error("Error generating sales report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/sales/list";
        }
    }

    @GetMapping("/summary")
    public String salesSummary(@RequestParam(required = false) Long warehouseId,
                               @RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               Model model) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);

            // Set default date range (current month)
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            ReportSummaryDTO summary = salesReportService.getSalesSummary(
                    warehouseId, customerId, startDate, endDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());
            model.addAttribute("customers", customerService.getAllCustomers());

            // Add data to model
            model.addAttribute("summary", summary);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("selectedCustomerId", customerId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/sales/summary";
        } catch (Exception e) {
            log.error("Error generating sales summary", e);
            model.addAttribute("error", "Error generating summary: " + e.getMessage());
            return "reports/sales/summary";
        }
    }
}
