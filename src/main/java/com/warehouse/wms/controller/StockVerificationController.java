package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.dto.StockAdjustmentDTO;
import com.warehouse.wms.dto.StockCurrentDTO;
import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.enums.AdjustmentType;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.StockAdjustmentService;
import com.warehouse.wms.service.StockService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/stock/verification")
@RequiredArgsConstructor
@Slf4j
public class StockVerificationController {

    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final StockAdjustmentService adjustmentService;

    @GetMapping
    public String showVerificationForm(Model model) {
        List<WarehouseDTO> warehouses = SecurityUtils.isAdmin()
            ? warehouseService.getAllWarehouses()
            : warehouseService.getWarehousesByCurrentUser();

        model.addAttribute("warehouses", warehouses);
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "Physical Stock Verification");

        return "stock/verification/form";
    }

    @GetMapping("/products")
    @ResponseBody
    public List<StockCurrentDTO> getWarehouseProducts(@RequestParam Long warehouseId) {
        SecurityUtils.validateWarehouseAccess(warehouseId);
        return stockService.getStockByWarehouse(warehouseId);
    }

    @PostMapping("/verify")
    public String verifyStock(@RequestParam Long warehouseId,
                             @RequestParam Map<String, String> physicalCounts,
                             RedirectAttributes redirectAttributes) {
        try {
            SecurityUtils.validateWarehouseAccess(warehouseId);
            Long currentUserId = SecurityUtils.getCurrentUserId();

            List<StockCurrentDTO> warehouseStock = stockService.getStockByWarehouse(warehouseId);
            List<StockAdjustmentDTO> adjustmentsCreated = new ArrayList<>();

            int discrepancies = 0;

            for (StockCurrentDTO stock : warehouseStock) {
                String key = "physical_" + stock.getProductId();
                String physicalCountStr = physicalCounts.get(key);

                if (physicalCountStr != null && !physicalCountStr.trim().isEmpty()) {
                    BigDecimal physicalCount = new BigDecimal(physicalCountStr);
                    BigDecimal systemCount = stock.getCurrentQuantity();
                    BigDecimal difference = physicalCount.subtract(systemCount);

                    if (difference.compareTo(BigDecimal.ZERO) != 0) {
                        discrepancies++;

                        // Create adjustment
                        StockAdjustmentDTO adjustmentDTO = StockAdjustmentDTO.builder()
                                .adjustmentDate(LocalDate.now())
                                .warehouseId(warehouseId)
                                .productId(stock.getProductId())
                                .adjustmentType(AdjustmentType.PHYSICAL_COUNT)
                                .adjustmentQuantity(difference)
                                .reason(String.format("Physical stock verification - System: %s, Physical: %s, Difference: %s",
                                        systemCount, physicalCount, difference))
                                .build();

                        StockAdjustmentDTO created = adjustmentService.createAdjustment(adjustmentDTO, currentUserId);
                        adjustmentsCreated.add(created);
                    }
                }
            }

            if (discrepancies > 0) {
                redirectAttributes.addFlashAttribute("success",
                    String.format("Stock verification completed. %d discrepancy adjustments created and pending approval.",
                            discrepancies));
            } else {
                redirectAttributes.addFlashAttribute("success",
                    "Stock verification completed. No discrepancies found!");
            }

            return "redirect:/stock/adjustments/pending";

        } catch (Exception e) {
            log.error("Error during stock verification", e);
            redirectAttributes.addFlashAttribute("error", "Error during verification: " + e.getMessage());
            return "redirect:/stock/verification";
        }
    }
}
