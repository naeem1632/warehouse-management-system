package com.warehouse.wms.controller;

import com.warehouse.wms.dto.CustomerDTO;
import com.warehouse.wms.dto.CustomerLedgerDTO;
import com.warehouse.wms.service.CustomerService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public String listCustomers(Model model) {
        try {
            List<CustomerDTO> customers = customerService.getAllCustomers();
            model.addAttribute("customers", customers);
            return "customers/list";
        } catch (Exception e) {
            log.error("Error listing customers", e);
            model.addAttribute("error", "Error loading customers: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        CustomerDTO customer = new CustomerDTO();
        customer.setOpeningDate(LocalDate.now());
        customer.setStatus("ACTIVE");

        model.addAttribute("customer", customer);
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CustomerDTO customer = customerService.getCustomerById(id);
            model.addAttribute("customer", customer);
            return "customers/form";
        } catch (Exception e) {
            log.error("Error loading customer for edit", e);
            redirectAttributes.addFlashAttribute("error", "Customer not found: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CustomerDTO customer = customerService.getCustomerById(id);
            model.addAttribute("customer", customer);
            return "customers/view";
        } catch (Exception e) {
            log.error("Error viewing customer", e);
            redirectAttributes.addFlashAttribute("error", "Customer not found: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @GetMapping("/{id}/ledger")
    public String viewCustomerLedger(@PathVariable Long id,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            CustomerDTO customer = customerService.getCustomerById(id);

            List<CustomerLedgerDTO> ledgerEntries;
            if (startDate != null && endDate != null) {
                ledgerEntries = customerService.getCustomerLedgerByDateRange(id, startDate, endDate);
            } else {
                ledgerEntries = customerService.getCustomerLedger(id);
            }

            model.addAttribute("customer", customer);
            model.addAttribute("ledgerEntries", ledgerEntries);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "customers/ledger";
        } catch (Exception e) {
            log.error("Error viewing customer ledger", e);
            redirectAttributes.addFlashAttribute("error", "Error loading ledger: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @PostMapping
    public String createCustomer(@ModelAttribute CustomerDTO customerDTO,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            CustomerDTO created = customerService.createCustomer(customerDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success",
                "Customer created successfully: " + created.getName());
            return "redirect:/customers/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating customer", e);
            redirectAttributes.addFlashAttribute("error", "Error creating customer: " + e.getMessage());
            return "redirect:/customers/create";
        }
    }

    @PostMapping("/{id}")
    public String updateCustomer(@PathVariable Long id,
                                @ModelAttribute CustomerDTO customerDTO,
                                RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            customerService.updateCustomer(id, customerDTO, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Customer updated successfully");
            return "redirect:/customers/" + id;
        } catch (Exception e) {
            log.error("Error updating customer", e);
            redirectAttributes.addFlashAttribute("error", "Error updating customer: " + e.getMessage());
            return "redirect:/customers/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            customerService.deleteCustomer(id, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Customer deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting customer", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }
}
