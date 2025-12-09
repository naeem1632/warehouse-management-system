package com.warehouse.wms.service;

import com.warehouse.wms.dto.ProductCategoryDTO;
import com.warehouse.wms.entity.ProductCategory;
import com.warehouse.wms.enums.CategoryStatus;
import com.warehouse.wms.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    public List<ProductCategoryDTO> getAllCategories() {
        return categoryRepository.findAllOrderByName().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<ProductCategoryDTO> getActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public ProductCategoryDTO getCategoryById(Long id) {
        ProductCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return convertToDTO(category);
    }

    public ProductCategoryDTO createCategory(ProductCategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Category with name '" + dto.getName() + "' already exists");
        }

        ProductCategory category = ProductCategory.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .status(dto.getStatus() != null ? dto.getStatus() : CategoryStatus.ACTIVE)
            .build();

        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        ProductCategory saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO dto) {
        ProductCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (!category.getName().equals(dto.getName()) &&
            categoryRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new RuntimeException("Category with name '" + dto.getName() + "' already exists");
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setStatus(dto.getStatus());
        category.setUpdatedAt(LocalDateTime.now());

        ProductCategory updated = categoryRepository.save(category);
        return convertToDTO(updated);
    }

    public void deleteCategory(Long id) {
        ProductCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    private ProductCategoryDTO convertToDTO(ProductCategory category) {
        return ProductCategoryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .status(category.getStatus())
            .build();
    }
}
