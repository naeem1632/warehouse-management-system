package com.warehouse.wms.controller;

import com.warehouse.wms.dto.PurchaseDTO;
import com.warehouse.wms.dto.PurchaseItemDTO;
import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.PaymentMethod;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.enums.PurchaseStatus;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.PurchaseService;
import com.warehouse.wms.service.SupplierService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/purchases")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping
    public String listPurchases(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Long warehouseId,
                               @RequestParam(required = false) Long supplierId,
                               Model model) {
        try {
            // Get accessible warehouses based on user role
            List<Warehouse> accessibleWarehouses;
            if (SecurityUtils.isAdmin()) {
                accessibleWarehouses = warehouseService.getActiveWarehouses();
            } else {
                Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
                accessibleWarehouses = new ArrayList<>(userWarehouses);
            }

            // Validate warehouse access if specific warehouse is requested
            if (warehouseId != null) {
                SecurityUtils.validateWarehouseAccess(warehouseId);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("purchaseDate").descending());
            Page<PurchaseDTO> purchasesPage;

            // For non-admin users, filter by their assigned warehouses
            if (!SecurityUtils.isAdmin()) {
                if (accessibleWarehouses.isEmpty()) {
                    // No accessible warehouses
                    purchasesPage = Page.empty(pageable);
                } else if (warehouseId != null) {
                    // Specific warehouse requested
                    purchasesPage = purchaseService.getPurchasesByWarehouse(warehouseId, pageable);
                } else {
                    // Show purchases from all accessible warehouses
                    // For now, use first warehouse or we'd need a new service method
                    // Better approach: add getPurchasesByWarehouses method in service
                    if (!accessibleWarehouses.isEmpty()) {
                        warehouseId = accessibleWarehouses.get(0).getId();
                        purchasesPage = purchaseService.getPurchasesByWarehouse(warehouseId, pageable);
                    } else {
                        purchasesPage = Page.empty(pageable);
                    }
                }
            } else {
                // Admin users: show all or filter as requested
                if (warehouseId != null) {
                    purchasesPage = purchaseService.getPurchasesByWarehouse(warehouseId, pageable);
                    model.addAttribute("warehouseId", warehouseId);
                } else if (supplierId != null) {
                    purchasesPage = purchaseService.getPurchasesBySupplier(supplierId, pageable);
                    model.addAttribute("supplierId", supplierId);
                } else {
                    purchasesPage = purchaseService.getAllPurchases(pageable);
                }
            }

            model.addAttribute("purchases", purchasesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", purchasesPage.getTotalPages());
            model.addAttribute("totalItems", purchasesPage.getTotalElements());
            model.addAttribute("selectedWarehouseId", warehouseId);

            // For filters - only show accessible warehouses
            model.addAttribute("warehouses", accessibleWarehouses);
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());

            return "purchases/list";
        } catch (Exception e) {
            log.error("Error listing purchases", e);
            model.addAttribute("error", "Error loading purchases: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // Get accessible warehouses based on user role
        List<Warehouse> accessibleWarehouses;
        if (SecurityUtils.isAdmin()) {
            accessibleWarehouses = warehouseService.getActiveWarehouses();
        } else {
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            accessibleWarehouses = new ArrayList<>(userWarehouses);
        }

        PurchaseDTO purchase = new PurchaseDTO();
        purchase.setPurchaseDate(LocalDate.now());

        model.addAttribute("purchase", purchase);
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "purchases/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            PurchaseDTO purchase = purchaseService.getPurchaseById(id);

            // Validate warehouse access
            SecurityUtils.validateWarehouseAccess(purchase.getWarehouseId());

            // Note: Removed restriction on editing completed purchases
            // All edits are now allowed and will be logged in the audit trail

            // Get accessible warehouses
            List<Warehouse> accessibleWarehouses;
            if (SecurityUtils.isAdmin()) {
                accessibleWarehouses = warehouseService.getActiveWarehouses();
            } else {
                Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
                accessibleWarehouses = new ArrayList<>(userWarehouses);
            }

            model.addAttribute("purchase", purchase);
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            model.addAttribute("warehouses", accessibleWarehouses);
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("paymentMethods", PaymentMethod.values());

            return "purchases/form";
        } catch (Exception e) {
            log.error("Error loading purchase for edit", e);
            redirectAttributes.addFlashAttribute("error", "Purchase not found: " + e.getMessage());
            return "redirect:/purchases";
        }
    }

    @GetMapping("/{id}")
    public String viewPurchase(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            PurchaseDTO purchase = purchaseService.getPurchaseById(id);

            // Validate warehouse access
            SecurityUtils.validateWarehouseAccess(purchase.getWarehouseId());

            model.addAttribute("purchase", purchase);
            return "purchases/view";
        } catch (Exception e) {
            log.error("Error viewing purchase", e);
            redirectAttributes.addFlashAttribute("error", "Purchase not found: " + e.getMessage());
            return "redirect:/purchases";
        }
    }

    @PostMapping
    public String createPurchase(@ModelAttribute PurchaseDTO purchaseDTO,
                                @RequestParam(required = false) List<Long> productIds,
                                @RequestParam(required = false) List<String> quantities,
                                @RequestParam(required = false) List<String> rates,
                                RedirectAttributes redirectAttributes) {
        try {
            // Validate warehouse access before creating
            SecurityUtils.validateWarehouseAccess(purchaseDTO.getWarehouseId());

            // Build purchase items from form arrays
            if (productIds != null && !productIds.isEmpty()) {
                for (int i = 0; i < productIds.size(); i++) {
                    if (productIds.get(i) != null) {
                        PurchaseItemDTO item = PurchaseItemDTO.builder()
                                .productId(productIds.get(i))
                                .quantity(new java.math.BigDecimal(quantities.get(i)))
                                .rate(new java.math.BigDecimal(rates.get(i)))
                                .amount(new java.math.BigDecimal(quantities.get(i))
                                        .multiply(new java.math.BigDecimal(rates.get(i))))
                                .build();
                        purchaseDTO.getItems().add(item);
                    }
                }
            }

            Long currentUserId = SecurityUtils.getCurrentUserId();
            PurchaseDTO created = purchaseService.createPurchase(purchaseDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success",
                "Purchase created successfully: " + created.getPurchaseNumber());
            return "redirect:/purchases/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating purchase", e);
            redirectAttributes.addFlashAttribute("error", "Error creating purchase: " + e.getMessage());
            return "redirect:/purchases/create";
        }
    }

    @PostMapping("/{id}")
    public String updatePurchase(@PathVariable Long id,
                                @ModelAttribute PurchaseDTO purchaseDTO,
                                RedirectAttributes redirectAttributes) {
        try {
            // Validate warehouse access before updating
            SecurityUtils.validateWarehouseAccess(purchaseDTO.getWarehouseId());

            Long currentUserId = SecurityUtils.getCurrentUserId();
            purchaseService.updatePurchase(id, purchaseDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Purchase updated successfully");
            return "redirect:/purchases/" + id;
        } catch (Exception e) {
            log.error("Error updating purchase", e);
            redirectAttributes.addFlashAttribute("error", "Error updating purchase: " + e.getMessage());
            return "redirect:/purchases/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePurchase(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Get the purchase to check warehouse access
            PurchaseDTO purchase = purchaseService.getPurchaseById(id);
            SecurityUtils.validateWarehouseAccess(purchase.getWarehouseId());

            Long currentUserId = SecurityUtils.getCurrentUserId();
            purchaseService.deletePurchase(id, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Purchase deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting purchase", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting purchase: " + e.getMessage());
        }
        return "redirect:/purchases";
    }
}