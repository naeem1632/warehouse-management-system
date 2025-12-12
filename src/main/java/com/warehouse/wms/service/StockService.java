package com.warehouse.wms.service;

import com.warehouse.wms.dto.StockCurrentDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.*;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.repository.*;
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
public class StockService {

    private final StockMovementRepository movementRepository;
    private final StockCurrentRepository currentRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    /**
     * Record a stock movement and update current stock
     */
    public StockMovementDTO recordMovement(StockMovementDTO dto, Long currentUserId) {
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
            .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

        // Get current stock
        StockCurrent currentStock = currentRepository
            .findByWarehouseAndProduct(warehouse, product)
            .orElse(StockCurrent.builder()
                .warehouse(warehouse)
                .product(product)
                .currentQuantity(BigDecimal.ZERO)
                .build());

        BigDecimal quantityBefore = currentStock.getCurrentQuantity();
        BigDecimal quantityAfter = quantityBefore.add(dto.getQuantity());

        // Validate stock (cannot go negative unless it's an adjustment)
        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0 &&
            dto.getMovementType() != MovementType.ADJUSTMENT) {
            throw new RuntimeException("Insufficient stock. Available: " + quantityBefore +
                ", Required: " + dto.getQuantity().abs());
        }

        // Create stock movement
        StockMovement movement = StockMovement.builder()
            .warehouse(warehouse)
            .product(product)
            .movementType(dto.getMovementType())
            .quantity(dto.getQuantity())
            .rate(dto.getRate())
            .referenceType(dto.getReferenceType())
            .referenceId(dto.getReferenceId())
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .notes(dto.getNotes())
            .createdBy(currentUserId)
            .createdAt(LocalDateTime.now())
            .build();

        StockMovement savedMovement = movementRepository.save(movement);

        // Update current stock
        currentStock.setCurrentQuantity(quantityAfter);
        currentStock.setLastMovement(savedMovement);
        currentStock.setLastUpdated(LocalDateTime.now());
        currentRepository.save(currentStock);

        return convertMovementToDTO(savedMovement);
    }

    public List<StockCurrentDTO> getCurrentStockByWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        return currentRepository.findByWarehouse(warehouse).stream()
            .map(this::convertCurrentToDTO)
            .collect(Collectors.toList());
    }

    public List<StockCurrentDTO> getLowStockItems(Long warehouseId) {
        return currentRepository.findLowStockItems(warehouseId).stream()
            .map(this::convertCurrentToDTO)
            .collect(Collectors.toList());
    }

    public List<StockMovementDTO> getMovementHistory(Long warehouseId, Long productId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        return movementRepository.findByWarehouseAndProductOrderByCreatedAtDesc(warehouse, product).stream()
            .map(this::convertMovementToDTO)
            .collect(Collectors.toList());
    }

    public BigDecimal getAvailableStock(Long warehouseId, Long productId) {
        return currentRepository.findByWarehouseIdAndProductId(warehouseId, productId)
            .map(StockCurrent::getCurrentQuantity)
            .orElse(BigDecimal.ZERO);
    }

    private StockMovementDTO convertMovementToDTO(StockMovement movement) {
        return StockMovementDTO.builder()
            .id(movement.getId())
            .warehouseId(movement.getWarehouse().getId())
            .warehouseName(movement.getWarehouse().getName())
            .productId(movement.getProduct().getId())
            .productName(movement.getProduct().getName())
            .productSku(movement.getProduct().getSku())
            .movementType(movement.getMovementType())
            .quantity(movement.getQuantity())
            .rate(movement.getRate())
            .referenceType(movement.getReferenceType())
            .referenceId(movement.getReferenceId())
            .quantityBefore(movement.getQuantityBefore())
            .quantityAfter(movement.getQuantityAfter())
            .notes(movement.getNotes())
            .createdBy(movement.getCreatedBy())
            .createdAt(movement.getCreatedAt())
            .build();
    }

    private StockCurrentDTO convertCurrentToDTO(StockCurrent current) {
        Product product = current.getProduct();

        return StockCurrentDTO.builder()
            .id(current.getId())
            .warehouseId(current.getWarehouse().getId())
            .warehouseName(current.getWarehouse().getName())
            .productId(product.getId())
            .productName(product.getName())
            .productSku(product.getSku())
            .productUnit(product.getUnit().getDisplayName())
            .currentQuantity(current.getCurrentQuantity())
            .lastUpdated(current.getLastUpdated())
            .build();
    }
}
