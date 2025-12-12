package com.warehouse.wms.service;

import com.warehouse.wms.dto.StockAdjustmentDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.StockAdjustment;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.AdjustmentStatus;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.repository.StockAdjustmentRepository;
import com.warehouse.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final UserService userService;

    @Transactional
    public StockAdjustmentDTO createAdjustment(StockAdjustmentDTO dto, Long currentUserId) {
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userService.getUserEntityById(currentUserId);

        // Get current stock quantity
        BigDecimal currentStock = stockService.getAvailableStock(warehouse.getId(), product.getId());

        // Calculate quantity after adjustment
        BigDecimal quantityAfter = currentStock.add(dto.getAdjustmentQuantity());

        // Validate quantity after is not negative
        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Adjustment would result in negative stock. Current stock: " + currentStock);
        }

        // Generate adjustment number
        String adjustmentNumber = generateAdjustmentNumber();

        StockAdjustment adjustment = StockAdjustment.builder()
                .adjustmentNumber(adjustmentNumber)
                .adjustmentDate(dto.getAdjustmentDate() != null ? dto.getAdjustmentDate() : LocalDate.now())
                .warehouse(warehouse)
                .product(product)
                .adjustmentType(dto.getAdjustmentType())
                .quantityBefore(currentStock)
                .adjustmentQuantity(dto.getAdjustmentQuantity())
                .quantityAfter(quantityAfter)
                .reason(dto.getReason())
                .status(AdjustmentStatus.PENDING)
                .createdBy(user)
                .build();

        adjustment = adjustmentRepository.save(adjustment);

        log.info("Stock adjustment created: {} for product: {} by user: {}",
                adjustmentNumber, product.getSku(), currentUserId);

        return convertToDTO(adjustment);
    }

    @Transactional
    public StockAdjustmentDTO approveAdjustment(Long adjustmentId, Long currentUserId) {
        StockAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Adjustment not found"));

        if (adjustment.getStatus() != AdjustmentStatus.PENDING) {
            throw new RuntimeException("Only pending adjustments can be approved");
        }

        User approver = userService.getUserEntityById(currentUserId);

        // Update adjustment status
        adjustment.setStatus(AdjustmentStatus.APPROVED);
        adjustment.setApprovedBy(approver);
        adjustment.setApprovedAt(LocalDateTime.now());

        adjustment = adjustmentRepository.save(adjustment);

        // Create stock movement
        StockMovementDTO movementDTO = StockMovementDTO.builder()
                .warehouseId(adjustment.getWarehouse().getId())
                .productId(adjustment.getProduct().getId())
                .movementType(MovementType.ADJUSTMENT)
                .quantity(adjustment.getAdjustmentQuantity())
                .rate(BigDecimal.ZERO)
                .referenceType("adjustment")
                .referenceId(adjustment.getId())
                .notes("Adjustment: " + adjustment.getAdjustmentType() + " - " + adjustment.getReason())
                .build();

        stockService.recordMovement(movementDTO, currentUserId);

        log.info("Stock adjustment approved: {} by user: {}",
                adjustment.getAdjustmentNumber(), currentUserId);

        return convertToDTO(adjustment);
    }

    @Transactional
    public StockAdjustmentDTO rejectAdjustment(Long adjustmentId, Long currentUserId) {
        StockAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Adjustment not found"));

        if (adjustment.getStatus() != AdjustmentStatus.PENDING) {
            throw new RuntimeException("Only pending adjustments can be rejected");
        }

        User approver = userService.getUserEntityById(currentUserId);

        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setApprovedBy(approver);
        adjustment.setApprovedAt(LocalDateTime.now());

        adjustment = adjustmentRepository.save(adjustment);

        log.info("Stock adjustment rejected: {} by user: {}",
                adjustment.getAdjustmentNumber(), currentUserId);

        return convertToDTO(adjustment);
    }

    @Transactional(readOnly = true)
    public StockAdjustmentDTO getAdjustmentById(Long id) {
        StockAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adjustment not found"));
        return convertToDTO(adjustment);
    }

    @Transactional(readOnly = true)
    public Page<StockAdjustmentDTO> getAllAdjustments(Pageable pageable) {
        return adjustmentRepository.findAllByOrderByAdjustmentDateDesc(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<StockAdjustmentDTO> getAdjustmentsWithFilters(Long warehouseId, Long productId,
                                                              AdjustmentStatus status, LocalDate startDate,
                                                              LocalDate endDate, Pageable pageable) {
        return adjustmentRepository.findWithFilters(warehouseId, productId, status, startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentDTO> getPendingAdjustments() {
        return adjustmentRepository.findByStatusOrderByAdjustmentDateDesc(AdjustmentStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getPendingCount() {
        return adjustmentRepository.countByStatus(AdjustmentStatus.PENDING);
    }

    private String generateAdjustmentNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ADJ-" + today + "-";

        StockAdjustment latest = adjustmentRepository.findLatestByAdjustmentNumberPrefix(prefix)
                .orElse(null);

        int nextSequence = 1;
        if (latest != null) {
            String lastNumber = latest.getAdjustmentNumber();
            String sequencePart = lastNumber.substring(lastNumber.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        return prefix + String.format("%04d", nextSequence);
    }

    private StockAdjustmentDTO convertToDTO(StockAdjustment adjustment) {
        return StockAdjustmentDTO.builder()
                .id(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .adjustmentDate(adjustment.getAdjustmentDate())
                .warehouseId(adjustment.getWarehouse().getId())
                .warehouseName(adjustment.getWarehouse().getName())
                .productId(adjustment.getProduct().getId())
                .productName(adjustment.getProduct().getName())
                .productSku(adjustment.getProduct().getSku())
                .productUnit(adjustment.getProduct().getUnit().getDisplayName())
                .adjustmentType(adjustment.getAdjustmentType())
                .quantityBefore(adjustment.getQuantityBefore())
                .adjustmentQuantity(adjustment.getAdjustmentQuantity())
                .quantityAfter(adjustment.getQuantityAfter())
                .reason(adjustment.getReason())
                .status(adjustment.getStatus())
                .createdBy(adjustment.getCreatedBy() != null ? adjustment.getCreatedBy().getId() : null)
                .createdByName(adjustment.getCreatedBy() != null ? adjustment.getCreatedBy().getName() : null)
                .approvedBy(adjustment.getApprovedBy() != null ? adjustment.getApprovedBy().getId() : null)
                .approvedByName(adjustment.getApprovedBy() != null ? adjustment.getApprovedBy().getName() : null)
                .approvedAt(adjustment.getApprovedAt())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}
