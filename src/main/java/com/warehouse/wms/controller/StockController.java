package com.warehouse.wms.controller;

import com.warehouse.wms.dto.StockCurrentDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.StockService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping("/overview")
    public String stockOverview(@RequestParam(required = false) Long warehouseId,
                               Model model) {
        // Get accessible warehouses based on user role
        List<Warehouse> accessibleWarehouses;
        if (SecurityUtils.isAdmin()) {
            accessibleWarehouses = warehouseService.getActiveWarehouses();
        } else {
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            accessibleWarehouses = new ArrayList<>(userWarehouses);
        }

        // If no warehouse specified or user doesn't have access, use first accessible warehouse
        if (warehouseId == null || !SecurityUtils.hasAccessToWarehouse(warehouseId)) {
            if (!accessibleWarehouses.isEmpty()) {
                warehouseId = accessibleWarehouses.get(0).getId();
            } else {
                model.addAttribute("errorMessage", "No accessible warehouses found");
                model.addAttribute("stockList", List.of());
                model.addAttribute("warehouses", accessibleWarehouses);
                return "stock/overview";
            }
        }

        // Validate warehouse access
        SecurityUtils.validateWarehouseAccess(warehouseId);

        List<StockCurrentDTO> stockList = stockService.getCurrentStockByWarehouse(warehouseId);
        model.addAttribute("stockList", stockList);
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("selectedWarehouseId", warehouseId);
        return "stock/overview";
    }

    @GetMapping("/low-stock")
    public String lowStockAlerts(@RequestParam(required = false) Long warehouseId, Model model) {
        // Get accessible warehouses based on user role
        List<Warehouse> accessibleWarehouses;
        if (SecurityUtils.isAdmin()) {
            accessibleWarehouses = warehouseService.getActiveWarehouses();
        } else {
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            accessibleWarehouses = new ArrayList<>(userWarehouses);
        }

        // If no warehouse specified or user doesn't have access, use first accessible warehouse
        if (warehouseId == null || !SecurityUtils.hasAccessToWarehouse(warehouseId)) {
            if (!accessibleWarehouses.isEmpty()) {
                warehouseId = accessibleWarehouses.get(0).getId();
            } else {
                model.addAttribute("errorMessage", "No accessible warehouses found");
                model.addAttribute("lowStockItems", List.of());
                model.addAttribute("warehouses", accessibleWarehouses);
                return "stock/low-stock";
            }
        }

        // Validate warehouse access
        SecurityUtils.validateWarehouseAccess(warehouseId);

        List<StockCurrentDTO> lowStockItems = stockService.getLowStockItems(warehouseId);
        model.addAttribute("lowStockItems", lowStockItems);
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("selectedWarehouseId", warehouseId);
        return "stock/low-stock";
    }

    @GetMapping("/movements")
    public String stockMovements(@RequestParam Long warehouseId,
                                 @RequestParam Long productId,
                                 Model model) {
        // Validate warehouse access
        SecurityUtils.validateWarehouseAccess(warehouseId);

        List<StockMovementDTO> movements = stockService.getMovementHistory(warehouseId, productId);
        model.addAttribute("movements", movements);
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("warehouse", warehouseService.getWarehouseById(warehouseId));
        return "stock/movements";
    }

    @GetMapping("/opening-balance/new")
    public String newOpeningBalanceForm(Model model) {
        // Get accessible warehouses based on user role
        List<Warehouse> accessibleWarehouses;
        if (SecurityUtils.isAdmin()) {
            accessibleWarehouses = warehouseService.getActiveWarehouses();
        } else {
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            accessibleWarehouses = new ArrayList<>(userWarehouses);
        }

        model.addAttribute("movement", new StockMovementDTO());
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("products", productService.getActiveProducts());
        return "stock/opening-balance-form";
    }

    @PostMapping("/opening-balance/save")
    public String saveOpeningBalance(@ModelAttribute StockMovementDTO dto,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Validate warehouse access before saving
            SecurityUtils.validateWarehouseAccess(dto.getWarehouseId());

            Long currentUserId = SecurityUtils.getCurrentUserId();

            dto.setMovementType(MovementType.OPENING);
            dto.setReferenceType("opening_balance");
            stockService.recordMovement(dto, currentUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Opening balance recorded successfully");
            return "redirect:/stock/overview?warehouseId=" + dto.getWarehouseId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/stock/opening-balance/new";
        }
    }
}
