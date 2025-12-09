package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProductCategoryDTO;
import com.warehouse.wms.enums.CategoryStatus;
import com.warehouse.wms.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "product-categories/list";
    }

    @GetMapping("/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new ProductCategoryDTO());
        model.addAttribute("isEdit", false);
        model.addAttribute("statuses", CategoryStatus.values());
        return "product-categories/form";
    }

    @GetMapping("/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        ProductCategoryDTO category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        model.addAttribute("isEdit", true);
        model.addAttribute("statuses", CategoryStatus.values());
        return "product-categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute ProductCategoryDTO dto,
                              RedirectAttributes redirectAttributes) {
        try {
            if (dto.getId() != null) {
                categoryService.updateCategory(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Category updated successfully");
            } else {
                categoryService.createCategory(dto);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Category created successfully");
            }
            return "redirect:/product-categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/product-categories/new";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Cannot delete category: " + e.getMessage());
        }
        return "redirect:/product-categories";
    }
}
