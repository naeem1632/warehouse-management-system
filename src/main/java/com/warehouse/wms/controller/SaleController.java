package com.warehouse.wms.controller;

import com.warehouse.wms.dto.SaleDTO;
import com.warehouse.wms.dto.SaleItemDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.PaymentMethod;
import com.warehouse.wms.service.CustomerService;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.SaleService;
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
@RequestMapping("/sales")
@RequiredArgsConstructor
@Slf4j
public class SaleController {

    private final SaleService saleService;
    private final CustomerService customerService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping
    public String listSales(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) Long customerId,
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

            Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());
            Page<SaleDTO> salesPage;

            // For non-admin users, filter by their assigned warehouses
            if (!SecurityUtils.isAdmin()) {
                if (accessibleWarehouses.isEmpty()) {
                    salesPage = Page.empty(pageable);
                } else if (warehouseId != null) {
                    salesPage = saleService.getSalesByWarehouse(warehouseId, pageable);
                } else {
                    if (!accessibleWarehouses.isEmpty()) {
                        warehouseId = accessibleWarehouses.get(0).getId();
                        salesPage = saleService.getSalesByWarehouse(warehouseId, pageable);
                    } else {
                        salesPage = Page.empty(pageable);
                    }
                }
            } else {
                // Admin users: show all or filter as requested
                if (warehouseId != null) {
                    salesPage = saleService.getSalesByWarehouse(warehouseId, pageable);
                    model.addAttribute("warehouseId", warehouseId);
                } else if (customerId != null) {
                    salesPage = saleService.getSalesByCustomer(customerId, pageable);
                    model.addAttribute("customerId", customerId);
                } else {
                    salesPage = saleService.getAllSales(pageable);
                }
            }

            model.addAttribute("sales", salesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", salesPage.getTotalPages());
            model.addAttribute("totalItems", salesPage.getTotalElements());
            model.addAttribute("selectedWarehouseId", warehouseId);

            // For filters
            model.addAttribute("warehouses", accessibleWarehouses);
            model.addAttribute("customers", customerService.getAllActiveCustomers());

            return "sales/list";
        } catch (Exception e) {
            log.error("Error listing sales", e);
            model.addAttribute("error", "Error loading sales: " + e.getMessage());
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

        SaleDTO sale = new SaleDTO();
        sale.setSaleDate(LocalDate.now());

        model.addAttribute("sale", sale);
        model.addAttribute("customers", customerService.getAllActiveCustomers());
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "sales/form";
    }

    @GetMapping("/{id}")
    public String viewSale(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SaleDTO sale = saleService.getSaleById(id);

            // Validate warehouse access
            SecurityUtils.validateWarehouseAccess(sale.getWarehouseId());

            model.addAttribute("sale", sale);
            return "sales/view";
        } catch (Exception e) {
            log.error("Error viewing sale", e);
            redirectAttributes.addFlashAttribute("error", "Sale not found: " + e.getMessage());
            return "redirect:/sales";
        }
    }

    @PostMapping
    public String createSale(@ModelAttribute SaleDTO saleDTO,
                            @RequestParam(required = false) List<Long> productIds,
                            @RequestParam(required = false) List<String> quantities,
                            @RequestParam(required = false) List<String> rates,
                            @RequestParam(required = false) List<String> discountAmounts,
                            RedirectAttributes redirectAttributes) {
        try {
            // Validate warehouse access before creating
            SecurityUtils.validateWarehouseAccess(saleDTO.getWarehouseId());

            // Build sale items from form arrays
            if (productIds != null && !productIds.isEmpty()) {
                for (int i = 0; i < productIds.size(); i++) {
                    if (productIds.get(i) != null) {
                        java.math.BigDecimal qty = new java.math.BigDecimal(quantities.get(i));
                        java.math.BigDecimal rate = new java.math.BigDecimal(rates.get(i));
                        java.math.BigDecimal discAmt = discountAmounts != null && i < discountAmounts.size()
                                ? new java.math.BigDecimal(discountAmounts.get(i))
                                : java.math.BigDecimal.ZERO;

                        java.math.BigDecimal amount = qty.multiply(rate).subtract(discAmt);

                        SaleItemDTO item = SaleItemDTO.builder()
                                .productId(productIds.get(i))
                                .quantity(qty)
                                .rate(rate)
                                .discountAmount(discAmt)
                                .amount(amount)
                                .build();
                        saleDTO.getItems().add(item);
                    }
                }
            }

            Long currentUserId = SecurityUtils.getCurrentUserId();
            SaleDTO created = saleService.createSale(saleDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success",
                "Sale created successfully: " + created.getSaleNumber() + " - Invoice: " + created.getInvoiceNumber());
            return "redirect:/sales/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating sale", e);
            redirectAttributes.addFlashAttribute("error", "Error creating sale: " + e.getMessage());
            return "redirect:/sales/create";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSale(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Get the sale to check warehouse access
            SaleDTO sale = saleService.getSaleById(id);
            SecurityUtils.validateWarehouseAccess(sale.getWarehouseId());

            Long currentUserId = SecurityUtils.getCurrentUserId();
            saleService.deleteSale(id, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Sale deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting sale", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting sale: " + e.getMessage());
        }
        return "redirect:/sales";
    }
}
