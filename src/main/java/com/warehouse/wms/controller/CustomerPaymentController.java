package com.warehouse.wms.controller;

import com.warehouse.wms.dto.CustomerDTO;
import com.warehouse.wms.dto.CustomerPaymentDTO;
import com.warehouse.wms.service.CustomerPaymentService;
import com.warehouse.wms.service.CustomerService;
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
@RequestMapping("/customer-payments")
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentController {

    private final CustomerPaymentService paymentService;
    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) Long customerId,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerPaymentDTO> paymentsPage = paymentService.getPaymentsWithFilters(
                customerId, startDate, endDate, pageable);

        List<CustomerDTO> customers = customerService.getAllActiveCustomers();

        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("customers", customers);
        model.addAttribute("customerId", customerId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paymentsPage.getTotalPages());
        model.addAttribute("totalItems", paymentsPage.getTotalElements());
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "Customer Payments");

        return "customer-payments/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        List<CustomerDTO> customers = customerService.getAllActiveCustomers();

        model.addAttribute("payment", new CustomerPaymentDTO());
        model.addAttribute("customers", customers);
        model.addAttribute("paymentMethods", List.of("CASH", "BANK", "CHEQUE"));
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "New Customer Payment");

        return "customer-payments/form";
    }

    @PostMapping
    public String createPayment(@ModelAttribute CustomerPaymentDTO paymentDTO,
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
            CustomerPaymentDTO created = paymentService.createPayment(paymentDTO, currentUserId);

            redirectAttributes.addFlashAttribute("success",
                "Payment received successfully: " + created.getReceiptNumber());
            return "redirect:/customer-payments/" + created.getId();

        } catch (Exception e) {
            log.error("Error creating customer payment", e);
            redirectAttributes.addFlashAttribute("error", "Error creating payment: " + e.getMessage());
            return "redirect:/customer-payments/create";
        }
    }

    @GetMapping("/{id}")
    public String viewPayment(@PathVariable Long id, Model model) {
        try {
            CustomerPaymentDTO payment = paymentService.getPaymentById(id);

            model.addAttribute("payment", payment);
            model.addAttribute("activePage", "payments");
            model.addAttribute("pageTitle", "Payment Receipt");

            return "customer-payments/view";

        } catch (Exception e) {
            log.error("Error viewing customer payment", e);
            model.addAttribute("error", "Payment not found");
            return "redirect:/customer-payments";
        }
    }
}
