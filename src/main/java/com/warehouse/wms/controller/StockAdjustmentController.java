package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.dto.StockAdjustmentDTO;
import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.enums.AdjustmentStatus;
import com.warehouse.wms.enums.AdjustmentType;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.StockAdjustmentService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/stock/adjustments")
@RequiredArgsConstructor
@Slf4j
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping
    public String list(@RequestParam(required = false) Long warehouseId,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) AdjustmentStatus status,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StockAdjustmentDTO> adjustmentsPage = adjustmentService.getAdjustmentsWithFilters(
                warehouseId, productId, status, startDate, endDate, pageable);

        List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses();
        List<ProductDTO> products = productService.getAllProducts();

        model.addAttribute("adjustments", adjustmentsPage.getContent());
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("products", products);
        model.addAttribute("statuses", AdjustmentStatus.values());
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("productId", productId);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adjustmentsPage.getTotalPages());
        model.addAttribute("totalItems", adjustmentsPage.getTotalElements());
        model.addAttribute("pendingCount", adjustmentService.getPendingCount());
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "Stock Adjustments");

        return "stock/adjustments/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        List<WarehouseDTO> warehouses = SecurityUtils.isAdmin()
            ? warehouseService.getAllWarehouses()
            : warehouseService.getWarehousesByCurrentUser();

        List<ProductDTO> products = productService.getAllProducts();

        model.addAttribute("adjustment", new StockAdjustmentDTO());
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("products", products);
        model.addAttribute("adjustmentTypes", Arrays.asList(AdjustmentType.values()));
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "New Stock Adjustment");

        return "stock/adjustments/form";
    }

    @PostMapping
    public String createAdjustment(@ModelAttribute StockAdjustmentDTO adjustmentDTO,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Validate warehouse access
            SecurityUtils.validateWarehouseAccess(adjustmentDTO.getWarehouseId());

            Long currentUserId = SecurityUtils.getCurrentUserId();
            StockAdjustmentDTO created = adjustmentService.createAdjustment(adjustmentDTO, currentUserId);

            redirectAttributes.addFlashAttribute("success",
                "Stock adjustment created successfully: " + created.getAdjustmentNumber() +
                ". Pending approval.");
            return "redirect:/stock/adjustments/" + created.getId();

        } catch (Exception e) {
            log.error("Error creating stock adjustment", e);
            redirectAttributes.addFlashAttribute("error", "Error creating adjustment: " + e.getMessage());
            return "redirect:/stock/adjustments/create";
        }
    }

    @GetMapping("/{id}")
    public String viewAdjustment(@PathVariable Long id, Model model) {
        try {
            StockAdjustmentDTO adjustment = adjustmentService.getAdjustmentById(id);

            model.addAttribute("adjustment", adjustment);
            model.addAttribute("activePage", "stock");
            model.addAttribute("pageTitle", "Adjustment Details");

            return "stock/adjustments/view";

        } catch (Exception e) {
            log.error("Error viewing stock adjustment", e);
            model.addAttribute("error", "Adjustment not found");
            return "redirect:/stock/adjustments";
        }
    }

    @PostMapping("/{id}/approve")
    public String approveAdjustment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            StockAdjustmentDTO approved = adjustmentService.approveAdjustment(id, currentUserId);

            redirectAttributes.addFlashAttribute("success",
                "Adjustment " + approved.getAdjustmentNumber() + " approved successfully. Stock updated.");
            return "redirect:/stock/adjustments/" + id;

        } catch (Exception e) {
            log.error("Error approving stock adjustment", e);
            redirectAttributes.addFlashAttribute("error", "Error approving adjustment: " + e.getMessage());
            return "redirect:/stock/adjustments/" + id;
        }
    }

    @PostMapping("/{id}/reject")
    public String rejectAdjustment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            StockAdjustmentDTO rejected = adjustmentService.rejectAdjustment(id, currentUserId);

            redirectAttributes.addFlashAttribute("success",
                "Adjustment " + rejected.getAdjustmentNumber() + " rejected.");
            return "redirect:/stock/adjustments/" + id;

        } catch (Exception e) {
            log.error("Error rejecting stock adjustment", e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting adjustment: " + e.getMessage());
            return "redirect:/stock/adjustments/" + id;
        }
    }

    @GetMapping("/pending")
    public String pendingAdjustments(Model model) {
        List<StockAdjustmentDTO> pending = adjustmentService.getPendingAdjustments();

        model.addAttribute("adjustments", pending);
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "Pending Adjustments");

        return "stock/adjustments/pending";
    }
}
