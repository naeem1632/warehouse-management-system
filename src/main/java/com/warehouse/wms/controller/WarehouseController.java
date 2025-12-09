package com.warehouse.wms.controller;

import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.WarehouseStatus;
import com.warehouse.wms.service.WarehouseService;
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

@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public String listWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Warehouse> warehousePage = warehouseService.getAllWarehouses(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("warehouses", warehousePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", warehousePage.getTotalPages());
        model.addAttribute("totalItems", warehousePage.getTotalElements());
        model.addAttribute("pageTitle", "Warehouse Management");
        model.addAttribute("activePage", "warehouses");

        return "warehouses/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("warehouseDTO", new WarehouseDTO());
        model.addAttribute("statuses", WarehouseStatus.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Add New Warehouse");
        model.addAttribute("activePage", "warehouses");
        return "warehouses/form";
    }

    @PostMapping
    public String createWarehouse(
            @Valid @ModelAttribute WarehouseDTO warehouseDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
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
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Add New Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Warehouse warehouse = warehouseService.getWarehouseById(id);

        WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .city(warehouse.getCity())
                .contactPerson(warehouse.getContactPerson())
                .phone(warehouse.getPhone())
                .email(warehouse.getEmail())
                .address(warehouse.getAddress())
                .status(warehouse.getStatus())
                .build();

        model.addAttribute("warehouseDTO", warehouseDTO);
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

        if (result.hasErrors()) {
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
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statuses", WarehouseStatus.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Edit Warehouse");
            model.addAttribute("activePage", "warehouses");
            return "warehouses/form";
        }
    }

    @GetMapping("/{id}")
    public String viewWarehouse(@PathVariable Long id, Model model) {
        Warehouse warehouse = warehouseService.getWarehouseById(id);
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("pageTitle", "View Warehouse");
        model.addAttribute("activePage", "warehouses");
        return "warehouses/view";
    }

    @PostMapping("/{id}/delete")
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