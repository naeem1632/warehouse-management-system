package com.warehouse.wms.controller;

import com.warehouse.wms.dto.UserDTO;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.UserRole;
import com.warehouse.wms.enums.UserStatus;
import com.warehouse.wms.service.UserService;
import com.warehouse.wms.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<User> userPage = userService.getAllUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("activePage", "users");

        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
        model.addAttribute("isEdit", false);
        model.addAttribute("pageTitle", "Add New User");
        model.addAttribute("activePage", "users");
        return "users/form";
    }

    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("statuses", UserStatus.values());
            model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Add New User");
            model.addAttribute("activePage", "users");
            return "users/form";
        }

        try {
            userService.createUser(userDTO);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
            return "redirect:/users";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("statuses", UserStatus.values());
            model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Add New User");
            model.addAttribute("activePage", "users");
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);

        List<Long> warehouseIds = user.getWarehouses().stream()
                .map(Warehouse::getId)
                .collect(Collectors.toList());

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .phone(user.getPhone())
                .warehouseIds(warehouseIds)
                .build();

        model.addAttribute("userDTO", userDTO);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
        model.addAttribute("isEdit", true);
        model.addAttribute("pageTitle", "Edit User");
        model.addAttribute("activePage", "users");

        return "users/form";
    }

    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("statuses", UserStatus.values());
            model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Edit User");
            model.addAttribute("activePage", "users");
            return "users/form";
        }

        try {
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully");
            return "redirect:/users";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("statuses", UserStatus.values());
            model.addAttribute("warehouses", warehouseService.getActiveWarehouses());
            model.addAttribute("isEdit", true);
            model.addAttribute("pageTitle", "Edit User");
            model.addAttribute("activePage", "users");
            return "users/form";
        }
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "View User");
        model.addAttribute("activePage", "users");
        return "users/view";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }
}