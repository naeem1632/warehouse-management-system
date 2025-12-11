package com.warehouse.wms.service;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.Supplier;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.ProductStatus;
import com.warehouse.wms.enums.ProductUnit;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.repository.SupplierRepository;
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
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;

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

        // Get supplier from warehouse - products inherit warehouse's supplier
        Supplier supplier = warehouse.getSupplier();
        if (supplier == null) {
            throw new RuntimeException("Warehouse '" + warehouse.getName() + "' does not have an assigned supplier");
        }

        // If supplier ID is provided in DTO, validate it matches warehouse's supplier
        if (dto.getSupplierId() != null && !dto.getSupplierId().equals(supplier.getId())) {
            throw new RuntimeException("Product's supplier must match warehouse's supplier (" + supplier.getName() + ")");
        }

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

        Product product = Product.builder()
            .warehouse(warehouse)
            .supplier(supplier)
            .sku(dto.getSku())
            .name(dto.getName())
            .unit(dto.getUnit() != null ? dto.getUnit() : ProductUnit.PCS)
            .description(dto.getDescription())
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

        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setUnit(dto.getUnit());
        product.setDescription(dto.getDescription());
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
        // Get supplier from product, or inherit from warehouse if product's supplier is null
        Supplier supplier = product.getSupplier();
        if (supplier == null && product.getWarehouse() != null) {
            supplier = product.getWarehouse().getSupplier();
        }

        return ProductDTO.builder()
            .id(product.getId())
            .warehouseId(product.getWarehouse() != null ? product.getWarehouse().getId() : null)
            .warehouseName(product.getWarehouse() != null ? product.getWarehouse().getName() : null)
            .supplierId(supplier != null ? supplier.getId() : null)
            .supplierName(supplier != null ? supplier.getName() : null)
            .sku(product.getSku())
            .name(product.getName())
            .unit(product.getUnit())
            .description(product.getDescription())
            .status(product.getStatus())
            .build();
    }
}
