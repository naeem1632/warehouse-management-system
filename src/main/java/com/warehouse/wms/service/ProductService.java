package com.warehouse.wms.service;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.ProductCategory;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.ProductStatus;
import com.warehouse.wms.enums.ProductUnit;
import com.warehouse.wms.repository.ProductCategoryRepository;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.repository.WarehouseRepository;
import com.warehouse.wms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Get all products filtered by user's accessible warehouses
     */
    public List<ProductDTO> getAllProducts() {
        List<Long> warehouseIds = SecurityUtils.getWarehouseIdsForFiltering();

        if (warehouseIds.isEmpty()) {
            // Admin: return all products
            return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } else {
            // Non-admin: return products from accessible warehouses
            return productRepository.findByWarehouseIdIn(warehouseIds).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
    }

    /**
     * Get active products filtered by user's accessible warehouses
     */
    public List<ProductDTO> getActiveProducts() {
        List<Long> warehouseIds = SecurityUtils.getWarehouseIdsForFiltering();

        if (warehouseIds.isEmpty()) {
            // Admin: return all active products
            return productRepository.findActiveProductsOrderByName(ProductStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } else {
            // Non-admin: return active products from accessible warehouses
            return productRepository.findByWarehouseIdInAndStatus(warehouseIds, ProductStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
    }

    public List<ProductDTO> getAllActiveProducts() {
        return getActiveProducts();
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
            .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
        return convertToDTO(product);
    }

    /**
     * Search products filtered by user's accessible warehouses
     */
    public List<ProductDTO> searchProducts(String search) {
        List<Long> warehouseIds = SecurityUtils.getWarehouseIdsForFiltering();

        if (warehouseIds.isEmpty()) {
            // Admin: search all products
            return productRepository.searchProducts(search).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } else {
            // Non-admin: search products from accessible warehouses
            return productRepository.searchProductsByWarehouses(warehouseIds, search).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
    }

    /**
     * Create product with warehouse-specific duplicate checking
     */
    public ProductDTO createProduct(ProductDTO dto) {
        // Validate warehouse ID is provided
        if (dto.getWarehouseId() == null) {
            throw new RuntimeException("Warehouse ID is required");
        }

        // Validate warehouse exists
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
            .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + dto.getWarehouseId()));

        // Validate user has access to this warehouse
        SecurityUtils.validateWarehouseAccess(dto.getWarehouseId());

        // Check duplicate SKU in same warehouse
        if (productRepository.existsByWarehouseIdAndSku(dto.getWarehouseId(), dto.getSku())) {
            throw new RuntimeException("Product with SKU '" + dto.getSku() +
                                     "' already exists in warehouse '" + warehouse.getName() + "'");
        }

        // Check duplicate name (case-insensitive) in same warehouse
        if (productRepository.existsByWarehouseIdAndNameIgnoreCase(dto.getWarehouseId(), dto.getName())) {
            throw new RuntimeException("Product with name '" + dto.getName() +
                                     "' already exists in warehouse '" + warehouse.getName() +
                                     "'. Please use a different name or check existing products.");
        }

        ProductCategory category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
        }

        Product product = Product.builder()
            .warehouse(warehouse)
            .sku(dto.getSku())
            .name(dto.getName())
            .category(category)
            .unit(dto.getUnit() != null ? dto.getUnit() : ProductUnit.PCS)
            .description(dto.getDescription())
            .minimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : BigDecimal.ZERO)
            .status(dto.getStatus() != null ? dto.getStatus() : ProductStatus.ACTIVE)
            .build();

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    /**
     * Update product with warehouse-specific duplicate checking
     */
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Validate user has access to product's warehouse
        SecurityUtils.validateWarehouseAccess(product.getWarehouse().getId());

        // Check duplicate SKU in same warehouse (excluding current product)
        if (!product.getSku().equals(dto.getSku()) &&
            productRepository.existsByWarehouseIdAndSkuAndIdNot(product.getWarehouse().getId(), dto.getSku(), id)) {
            throw new RuntimeException("Product with SKU '" + dto.getSku() +
                                     "' already exists in this warehouse");
        }

        // Check duplicate name (case-insensitive) in same warehouse (excluding current product)
        if (!product.getName().equalsIgnoreCase(dto.getName()) &&
            productRepository.existsByWarehouseIdAndNameIgnoreCaseAndIdNot(
                product.getWarehouse().getId(), dto.getName(), id)) {
            throw new RuntimeException("Product with name '" + dto.getName() +
                                     "' already exists in this warehouse. Please use a different name.");
        }

        ProductCategory category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
        }

        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setCategory(category);
        product.setUnit(dto.getUnit());
        product.setDescription(dto.getDescription());
        product.setMinimumStock(dto.getMinimumStock());
        product.setStatus(dto.getStatus());
        product.setUpdatedAt(LocalDateTime.now());

        Product updated = productRepository.save(product);
        return convertToDTO(updated);
    }

    /**
     * Delete product with warehouse access validation
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Validate user has access to product's warehouse
        SecurityUtils.validateWarehouseAccess(product.getWarehouse().getId());

        productRepository.delete(product);
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
            .id(product.getId())
            .warehouseId(product.getWarehouse() != null ? product.getWarehouse().getId() : null)
            .warehouseName(product.getWarehouse() != null ? product.getWarehouse().getName() : null)
            .sku(product.getSku())
            .name(product.getName())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .unit(product.getUnit())
            .description(product.getDescription())
            .minimumStock(product.getMinimumStock())
            .status(product.getStatus())
            .build();
    }
}
