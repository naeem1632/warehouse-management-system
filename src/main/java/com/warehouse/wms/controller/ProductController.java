package com.warehouse.wms.controller;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.ProductStatus;
import com.warehouse.wms.enums.ProductUnit;
import com.warehouse.wms.service.ProductCategoryService;
import com.warehouse.wms.service.ProductService;
import com.warehouse.wms.service.WarehouseService;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryService categoryService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String listProducts(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.trim().isEmpty()) {
            model.addAttribute("products", productService.searchProducts(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("products", productService.getAllProducts());
        }
        return "products/list";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        // Get accessible warehouses for dropdown
        List<Warehouse> accessibleWarehouses;
        if (SecurityUtils.isAdmin()) {
            accessibleWarehouses = warehouseService.getActiveWarehouses();
        } else {
            Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
            accessibleWarehouses = new ArrayList<>(userWarehouses);
        }

        ProductDTO productDTO = new ProductDTO();
        // If user has only one warehouse, pre-select it
        if (accessibleWarehouses.size() == 1) {
            productDTO.setWarehouseId(accessibleWarehouses.get(0).getId());
        }

        model.addAttribute("product", productDTO);
        model.addAttribute("warehouses", accessibleWarehouses);
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("units", ProductUnit.values());
        model.addAttribute("statuses", ProductStatus.values());
        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProductDTO product = productService.getProductById(id);

            // Validate user has access to this product's warehouse
            SecurityUtils.validateWarehouseAccess(product.getWarehouseId());

            // Get accessible warehouses (for display, but warehouse shouldn't change on edit)
            List<Warehouse> accessibleWarehouses;
            if (SecurityUtils.isAdmin()) {
                accessibleWarehouses = warehouseService.getActiveWarehouses();
            } else {
                Set<Warehouse> userWarehouses = SecurityUtils.getCurrentUserWarehouses();
                accessibleWarehouses = new ArrayList<>(userWarehouses);
            }

            model.addAttribute("product", product);
            model.addAttribute("warehouses", accessibleWarehouses);
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("units", ProductUnit.values());
            model.addAttribute("statuses", ProductStatus.values());
            return "products/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products";
        }
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute ProductDTO dto,
                             RedirectAttributes redirectAttributes) {
        try {
            if (dto.getId() != null) {
                productService.updateProduct(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Product updated successfully");
            } else {
                productService.createProduct(dto);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Product created successfully");
            }
            return "redirect:/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return dto.getId() != null ?
                "redirect:/products/edit/" + dto.getId() :
                "redirect:/products/new";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Product deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Cannot delete product: " + e.getMessage());
        }
        return "redirect:/products";
    }
}
