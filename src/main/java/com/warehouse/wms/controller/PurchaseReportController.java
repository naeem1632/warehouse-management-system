package com.warehouse.wms.controller;

import com.warehouse.wms.dto.PurchaseReportDTO;
import com.warehouse.wms.dto.ReportSummaryDTO;
import com.warehouse.wms.service.PurchaseReportService;
import com.warehouse.wms.service.SupplierService;
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
@RequestMapping("/reports/purchases")
@RequiredArgsConstructor
@Slf4j
public class PurchaseReportController {

    private final PurchaseReportService purchaseReportService;
    private final WarehouseService warehouseService;
    private final SupplierService supplierService;

    @GetMapping
    public String purchaseReport(@RequestParam(required = false) Long warehouseId,
                                 @RequestParam(required = false) Long supplierId,
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
            List<PurchaseReportDTO> purchases = purchaseReportService.getPurchaseReport(
                    warehouseId, supplierId, startDate, endDate);

            ReportSummaryDTO summary = purchaseReportService.getPurchaseSummary(
                    warehouseId, supplierId, startDate, endDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());

            // Add data to model
            model.addAttribute("purchases", purchases);
            model.addAttribute("summary", summary);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("selectedSupplierId", supplierId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/purchases/list";
        } catch (Exception e) {
            log.error("Error generating purchase report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/purchases/list";
        }
    }

    @GetMapping("/summary")
    public String purchaseSummary(@RequestParam(required = false) Long warehouseId,
                                  @RequestParam(required = false) Long supplierId,
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

            ReportSummaryDTO summary = purchaseReportService.getPurchaseSummary(
                    warehouseId, supplierId, startDate, endDate);

            // Get filter options
            model.addAttribute("warehouses", warehouseService.getAllWarehouses());
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());

            // Add data to model
            model.addAttribute("summary", summary);
            model.addAttribute("selectedWarehouseId", warehouseId);
            model.addAttribute("selectedSupplierId", supplierId);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/purchases/summary";
        } catch (Exception e) {
            log.error("Error generating purchase summary", e);
            model.addAttribute("error", "Error generating summary: " + e.getMessage());
            return "reports/purchases/summary";
        }
    }

    @GetMapping("/by-supplier")
    public String purchasesBySupplier(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                      Model model) {
        try {
            // Set default date range (current month)
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<PurchaseReportDTO> purchases = purchaseReportService.getPurchasesBySupplier(startDate, endDate);

            model.addAttribute("purchases", purchases);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/purchases/by-supplier";
        } catch (Exception e) {
            log.error("Error generating purchases by supplier report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/purchases/by-supplier";
        }
    }

    @GetMapping("/by-warehouse")
    public String purchasesByWarehouse(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       Model model) {
        try {
            // Set default date range (current month)
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<PurchaseReportDTO> purchases = purchaseReportService.getPurchasesByWarehouse(startDate, endDate);

            model.addAttribute("purchases", purchases);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "reports/purchases/by-warehouse";
        } catch (Exception e) {
            log.error("Error generating purchases by warehouse report", e);
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "reports/purchases/by-warehouse";
        }
    }
}
