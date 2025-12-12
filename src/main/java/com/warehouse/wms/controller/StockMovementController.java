package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.StockService;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/stock/movements")
@RequiredArgsConstructor
@Slf4j
public class StockMovementController {

    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping
    public String movementHistory(@RequestParam(required = false) Long warehouseId,
                                  @RequestParam(required = false) Long productId,
                                  @RequestParam(required = false) MovementType movementType,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size,
                                  Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StockMovementDTO> movementsPage = stockService.getMovementsWithFilters(
                warehouseId, productId, movementType, startDate, endDate, pageable);

        List<WarehouseDTO> warehouses = SecurityUtils.isAdmin()
            ? warehouseService.getAllWarehouses()
            : warehouseService.getWarehousesByCurrentUser();

        List<ProductDTO> products = productService.getAllProducts();

        model.addAttribute("movements", movementsPage.getContent());
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("products", products);
        model.addAttribute("movementTypes", Arrays.asList(MovementType.values()));
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("productId", productId);
        model.addAttribute("movementType", movementType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movementsPage.getTotalPages());
        model.addAttribute("totalItems", movementsPage.getTotalElements());
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "Stock Movement History");

        return "stock/movements/history";
    }

    @GetMapping("/{warehouseId}/{productId}")
    public String productMovementHistory(@PathVariable Long warehouseId,
                                         @PathVariable Long productId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         Model model) {

        SecurityUtils.validateWarehouseAccess(warehouseId);

        List<StockMovementDTO> movements = stockService.getMovementsByWarehouseAndProduct(
                warehouseId, productId, startDate, endDate);

        WarehouseDTO warehouse = warehouseService.getWarehouseById(warehouseId);
        ProductDTO product = productService.getProductById(productId);

        model.addAttribute("movements", movements);
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("product", product);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activePage", "stock");
        model.addAttribute("pageTitle", "Product Movement History");

        return "stock/movements/product-history";
    }
}
