package com.warehouse.wms.controller;

import com.warehouse.wms.entity.User;
import com.warehouse.wms.repository.UserRepository;
import com.warehouse.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get statistics based on user role
        long totalWarehouses = warehouseRepository.count();
        long totalUsers = userRepository.count();

        model.addAttribute("user", user);
        model.addAttribute("totalWarehouses", totalWarehouses);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");

        return "dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
}