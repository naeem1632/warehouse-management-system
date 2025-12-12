package com.warehouse.wms.controller;

import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.dto.SupplierPaymentDTO;
import com.warehouse.wms.service.SupplierPaymentService;
import com.warehouse.wms.service.SupplierService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/supplier-payments")
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentController {

    private final SupplierPaymentService paymentService;
    private final SupplierService supplierService;

    @GetMapping
    public String list(@RequestParam(required = false) Long supplierId,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SupplierPaymentDTO> paymentsPage = paymentService.getPaymentsWithFilters(
                supplierId, startDate, endDate, pageable);

        List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();

        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paymentsPage.getTotalPages());
        model.addAttribute("totalItems", paymentsPage.getTotalElements());
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "Supplier Payments");

        return "supplier-payments/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();

        model.addAttribute("payment", new SupplierPaymentDTO());
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("paymentMethods", List.of("CASH", "BANK", "CHEQUE"));
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "New Supplier Payment");

        return "supplier-payments/form";
    }

    @PostMapping
    public String createPayment(@ModelAttribute SupplierPaymentDTO paymentDTO,
                                RedirectAttributes redirectAttributes) {
        try {
            // Validate amount
            if (paymentDTO.getAmount() == null || paymentDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Payment amount must be greater than zero");
            }

            // Set default payment date if not provided
            if (paymentDTO.getPaymentDate() == null) {
                paymentDTO.setPaymentDate(LocalDate.now());
            }

            Long currentUserId = SecurityUtils.getCurrentUserId();
            SupplierPaymentDTO created = paymentService.createPayment(paymentDTO, currentUserId);

            redirectAttributes.addFlashAttribute("success",
                "Payment recorded successfully: " + created.getPaymentNumber());
            return "redirect:/supplier-payments/" + created.getId();

        } catch (Exception e) {
            log.error("Error creating supplier payment", e);
            redirectAttributes.addFlashAttribute("error", "Error creating payment: " + e.getMessage());
            return "redirect:/supplier-payments/create";
        }
    }

    @GetMapping("/{id}")
    public String viewPayment(@PathVariable Long id, Model model) {
        try {
            SupplierPaymentDTO payment = paymentService.getPaymentById(id);

            model.addAttribute("payment", payment);
            model.addAttribute("activePage", "payments");
            model.addAttribute("pageTitle", "Payment Details");

            return "supplier-payments/view";

        } catch (Exception e) {
            log.error("Error viewing supplier payment", e);
            model.addAttribute("error", "Payment not found");
            return "redirect:/supplier-payments";
        }
    }
}
