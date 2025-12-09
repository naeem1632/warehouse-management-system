package com.warehouse.wms.controller;

import com.warehouse.wms.entity.User;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.UserRepository;
import com.warehouse.wms.repository.WarehouseRepository;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // Use username instead of email for authentication
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get statistics based on user role and assigned warehouses
        long totalWarehouses;
        long totalUsers;
        long totalProducts;
        long transactionsToday;

        if (SecurityUtils.isAdmin()) {
            // Admin sees all statistics
            totalWarehouses = warehouseRepository.count();
            totalUsers = userRepository.count();
            totalProducts = productRepository.count();
            transactionsToday = purchaseRepository.countPurchasesFromDate(LocalDate.now());
        } else {
            // Non-admin sees only their warehouse-specific statistics
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            totalWarehouses = userWarehouses.size();

            // Users: Only admins should see this stat, for non-admin show 0 or hide in template
            totalUsers = 0;

            // Products: Global entity, all users can see
            totalProducts = productRepository.count();

            // Transactions: Filter by user's warehouses
            List<Long> warehouseIds = SecurityUtils.getCurrentUserWarehouseIds();
            if (!warehouseIds.isEmpty()) {
                transactionsToday = purchaseRepository.countPurchasesByWarehousesAndDate(
                    warehouseIds, LocalDate.now());
            } else {
                transactionsToday = 0;
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("totalWarehouses", totalWarehouses);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("transactionsToday", transactionsToday);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");

        return "dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
}