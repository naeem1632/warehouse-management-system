package com.warehouse.wms.controller;

import com.warehouse.wms.dto.StockCurrentDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.StockService;
import com.warehouse.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping("/overview")
    public String stockOverview(@RequestParam(required = false) Long warehouseId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        // If no warehouse specified, use first available warehouse
        if (warehouseId == null) {
            warehouseId = 1L; // Default to first warehouse, should be improved
        }

        List<StockCurrentDTO> stockList = stockService.getCurrentStockByWarehouse(warehouseId);
        model.addAttribute("stockList", stockList);
        model.addAttribute("warehouses", warehouseService.getAllWarehouses());
        model.addAttribute("selectedWarehouseId", warehouseId);
        return "stock/overview";
    }

    @GetMapping("/low-stock")
    public String lowStockAlerts(@RequestParam(required = false) Long warehouseId, Model model) {
        if (warehouseId == null) {
            warehouseId = 1L;
        }

        List<StockCurrentDTO> lowStockItems = stockService.getLowStockItems(warehouseId);
        model.addAttribute("lowStockItems", lowStockItems);
        model.addAttribute("warehouses", warehouseService.getAllWarehouses());
        model.addAttribute("selectedWarehouseId", warehouseId);
        return "stock/low-stock";
    }

    @GetMapping("/movements")
    public String stockMovements(@RequestParam Long warehouseId,
                                 @RequestParam Long productId,
                                 Model model) {
        List<StockMovementDTO> movements = stockService.getMovementHistory(warehouseId, productId);
        model.addAttribute("movements", movements);
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("warehouse", warehouseService.getWarehouseById(warehouseId));
        return "stock/movements";
    }

    @GetMapping("/opening-balance/new")
    public String newOpeningBalanceForm(Model model) {
        model.addAttribute("movement", new StockMovementDTO());
        model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
        model.addAttribute("products", productService.getActiveProducts());
        return "stock/opening-balance-form";
    }

    @PostMapping("/opening-balance/save")
    public String saveOpeningBalance(@ModelAttribute StockMovementDTO dto,
                                    @AuthenticationPrincipal User currentUser,
                                    RedirectAttributes redirectAttributes) {
        try {
            dto.setMovementType(MovementType.OPENING);
            dto.setReferenceType("opening_balance");
            stockService.recordMovement(dto, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                "Opening balance recorded successfully");
            return "redirect:/stock/overview?warehouseId=" + dto.getWarehouseId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/stock/opening-balance/new";
        }
    }
}
