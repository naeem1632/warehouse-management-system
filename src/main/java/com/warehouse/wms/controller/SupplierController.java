package com.warehouse.wms.controller;

import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.dto.SupplierLedgerDTO;
import com.warehouse.wms.enums.BalanceType;
import com.warehouse.wms.enums.BusinessType;
import com.warehouse.wms.service.SupplierService;
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
import java.util.List;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public String listSuppliers(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String keyword,
                               Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<SupplierDTO> suppliersPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                suppliersPage = supplierService.searchSuppliers(keyword.trim(), pageable);
                model.addAttribute("keyword", keyword);
            } else {
                suppliersPage = supplierService.getAllSuppliers(pageable);
            }

            model.addAttribute("suppliers", suppliersPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", suppliersPage.getTotalPages());
            model.addAttribute("totalItems", suppliersPage.getTotalElements());

            return "suppliers/list";
        } catch (Exception e) {
            log.error("Error listing suppliers", e);
            model.addAttribute("error", "Error loading suppliers: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("supplier", new SupplierDTO());
        model.addAttribute("businessTypes", BusinessType.values());
        model.addAttribute("balanceTypes", BalanceType.values());
        model.addAttribute("statuses", new String[]{"ACTIVE", "INACTIVE"});
        return "suppliers/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SupplierDTO supplier = supplierService.getSupplierById(id);
            model.addAttribute("supplier", supplier);
            model.addAttribute("businessTypes", BusinessType.values());
            model.addAttribute("balanceTypes", BalanceType.values());
            model.addAttribute("statuses", new String[]{"ACTIVE", "INACTIVE"});
            return "suppliers/form";
        } catch (Exception e) {
            log.error("Error loading supplier for edit", e);
            redirectAttributes.addFlashAttribute("error", "Supplier not found: " + e.getMessage());
            return "redirect:/suppliers";
        }
    }

    @GetMapping("/{id}")
    public String viewSupplier(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SupplierDTO supplier = supplierService.getSupplierById(id);
            model.addAttribute("supplier", supplier);
            return "suppliers/view";
        } catch (Exception e) {
            log.error("Error viewing supplier", e);
            redirectAttributes.addFlashAttribute("error", "Supplier not found: " + e.getMessage());
            return "redirect:/suppliers";
        }
    }

    @PostMapping
    public String createSupplier(@ModelAttribute SupplierDTO supplierDTO,
                                RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            supplierService.createSupplier(supplierDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Supplier created successfully");
            return "redirect:/suppliers";
        } catch (Exception e) {
            log.error("Error creating supplier", e);
            redirectAttributes.addFlashAttribute("error", "Error creating supplier: " + e.getMessage());
            return "redirect:/suppliers/create";
        }
    }

    @PostMapping("/{id}")
    public String updateSupplier(@PathVariable Long id,
                                @ModelAttribute SupplierDTO supplierDTO,
                                RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            supplierService.updateSupplier(id, supplierDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Supplier updated successfully");
            return "redirect:/suppliers";
        } catch (Exception e) {
            log.error("Error updating supplier", e);
            redirectAttributes.addFlashAttribute("error", "Error updating supplier: " + e.getMessage());
            return "redirect:/suppliers/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            supplierService.deleteSupplier(id, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Supplier deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting supplier", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}/ledger")
    public String viewSupplierLedger(@PathVariable Long id,
                                    @RequestParam(required = false) LocalDate startDate,
                                    @RequestParam(required = false) LocalDate endDate,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            SupplierDTO supplier = supplierService.getSupplierById(id);
            model.addAttribute("supplier", supplier);

            List<SupplierLedgerDTO> ledger;
            if (startDate != null && endDate != null) {
                ledger = supplierService.getSupplierLedgerByDateRange(id, startDate, endDate);
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
            } else {
                ledger = supplierService.getSupplierLedger(id);
            }

            model.addAttribute("ledger", ledger);
            return "suppliers/ledger";
        } catch (Exception e) {
            log.error("Error viewing supplier ledger", e);
            redirectAttributes.addFlashAttribute("error", "Error loading ledger: " + e.getMessage());
            return "redirect:/suppliers";
        }
    }
}