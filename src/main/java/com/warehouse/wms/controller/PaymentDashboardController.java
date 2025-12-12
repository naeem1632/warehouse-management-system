package com.warehouse.wms.controller;

import com.warehouse.wms.dto.CustomerDTO;
import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.service.CustomerService;
import com.warehouse.wms.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentDashboardController {

    private final SupplierService supplierService;
    private final CustomerService customerService;

    @GetMapping("/outstanding")
    public String outstandingPaymentsDashboard(Model model) {
        // Get all active suppliers with outstanding balances
        List<SupplierDTO> allSuppliers = supplierService.getAllActiveSuppliers();
        List<SupplierDTO> suppliersWithBalance = allSuppliers.stream()
                .filter(s -> s.getCurrentBalance() != null && s.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0)
                .collect(Collectors.toList());

        // Get all active customers with outstanding balances
        List<CustomerDTO> allCustomers = customerService.getAllActiveCustomers();
        List<CustomerDTO> customersWithBalance = allCustomers.stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0)
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalPayables = BigDecimal.ZERO;
        BigDecimal totalReceivables = BigDecimal.ZERO;

        for (SupplierDTO supplier : suppliersWithBalance) {
            if ("DEBIT".equals(supplier.getCurrentBalanceType())) {
                totalPayables = totalPayables.add(supplier.getCurrentBalance());
            }
        }

        for (CustomerDTO customer : customersWithBalance) {
            if ("DEBIT".equals(customer.getCurrentBalanceType())) {
                totalReceivables = totalReceivables.add(customer.getCurrentBalance());
            }
        }

        model.addAttribute("suppliers", suppliersWithBalance);
        model.addAttribute("customers", customersWithBalance);
        model.addAttribute("totalPayables", totalPayables);
        model.addAttribute("totalReceivables", totalReceivables);
        model.addAttribute("netPosition", totalReceivables.subtract(totalPayables));
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "Outstanding Payments");

        return "payments/outstanding";
    }
}
