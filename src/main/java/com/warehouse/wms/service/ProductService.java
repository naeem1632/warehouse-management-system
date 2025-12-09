package com.warehouse.wms.service;

import com.warehouse.wms.dto.ProductDTO;
import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.ProductCategory;
import com.warehouse.wms.enums.ProductStatus;
import com.warehouse.wms.enums.ProductUnit;
import com.warehouse.wms.repository.ProductCategoryRepository;
import com.warehouse.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<ProductDTO> getActiveProducts() {
        return productRepository.findActiveProductsOrderByName(ProductStatus.ACTIVE).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
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

    public List<ProductDTO> searchProducts(String search) {
        return productRepository.searchProducts(search).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new RuntimeException("Product with SKU '" + dto.getSku() + "' already exists");
        }

        ProductCategory category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
        }

        Product product = Product.builder()
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

    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (!product.getSku().equals(dto.getSku()) &&
            productRepository.existsBySkuAndIdNot(dto.getSku(), id)) {
            throw new RuntimeException("Product with SKU '" + dto.getSku() + "' already exists");
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

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
            .id(product.getId())
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
