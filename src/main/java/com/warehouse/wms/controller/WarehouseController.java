package com.warehouse.wms.controller;

import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.WarehouseStatus;
import com.warehouse.wms.service.SupplierService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final SupplierService supplierService;

    @GetMapping
    public String listWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        List<Warehouse> warehouses;

        // Filter by user's assigned warehouses
        if (SecurityUtils.isAdmin()) {
            // Admin sees all warehouses
            Page<Warehouse> warehousePage = warehouseService.getAllWarehouses(
                    PageRequest.of(page, size, Sort.by("createdAt").descending())
            );
            warehouses = warehousePage.getContent();
            model.addAttribute("totalPages", warehousePage.getTotalPages());
            model.addAttribute("totalItems", warehousePage.getTotalElements());
        } else {
            // Non-admin only sees assigned warehouses
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            warehouses = new ArrayList<>(userWarehouses);
            warehouses.sort((w1, w2) -> w2.getCreatedAt().compareTo(w1.getCreatedAt()));
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", warehouses.size());
        }

        model.addAttribute("warehouses", warehouses);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", "Warehouse Management");
        model.addAttribute("activePage", "warehouses");

        return "warehouses/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
        model.addAttribute("warehouseDTO", new WarehouseDTO());
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("statuses", WarehouseStatus.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Add New Warehouse");
        model.addAttribute("activePage", "warehouses");
        return "warehouses/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createWarehouse(
            @Valid @ModelAttribute WarehouseDTO warehouseDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Add New Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }

        try {
            warehouseService.createWarehouse(warehouseDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse created successfully");
            return "redirect:/warehouses";
        } catch (RuntimeException e) {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Add New Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Validate warehouse access
        SecurityUtils.validateWarehouseAccess(id);

        Warehouse warehouse = warehouseService.getWarehouseById(id);

        WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .supplierId(warehouse.getSupplier() != null ? warehouse.getSupplier().getId() : null)
                .supplierName(warehouse.getSupplier() != null ? warehouse.getSupplier().getName() : null)
                .location(warehouse.getLocation())
                .city(warehouse.getCity())
                .contactPerson(warehouse.getContactPerson())
                .phone(warehouse.getPhone())
                .email(warehouse.getEmail())
                .address(warehouse.getAddress())
                .status(warehouse.getStatus())
                .build();

        List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
        model.addAttribute("warehouseDTO", warehouseDTO);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("statuses", WarehouseStatus.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Edit Warehouse");
        model.addAttribute("activePage", "warehouses");

        return "warehouses/form";
    }

    @PostMapping("/{id}")
    public String updateWarehouse(
            @PathVariable Long id,
            @Valid @ModelAttribute WarehouseDTO warehouseDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate warehouse access
        try {
            SecurityUtils.validateWarehouseAccess(id);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/warehouses";
        }

        if (result.hasErrors()) {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Edit Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }

        try {
            warehouseService.updateWarehouse(id, warehouseDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse updated successfully");
            return "redirect:/warehouses";
        } catch (RuntimeException e) {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Edit Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }
    }

    @GetMapping("/{id}")
    public String viewWarehouse(@PathVariable Long id, Model model) {
        // Validate warehouse access
        SecurityUtils.validateWarehouseAccess(id);

        Warehouse warehouse = warehouseService.getWarehouseById(id);
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("pageTitle", "View Warehouse");
        model.addAttribute("activePage", "warehouses");
        return "warehouses/view";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteWarehouse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warehouseService.deleteWarehouse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Warehouse deleted successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/warehouses";
    }
}