package com.warehouse.wms.service;

import com.warehouse.wms.dto.PurchaseDTO;
import com.warehouse.wms.dto.PurchaseItemDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.*;
import com.warehouse.wms.enums.AuditAction;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.enums.PurchaseStatus;
import com.warehouse.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final StockService stockService;
    private final SupplierService supplierService;
    private final PurchaseAuditService purchaseAuditService;

    @Transactional(readOnly = true)
    public Page<PurchaseDTO> getAllPurchases(Pageable pageable) {
        return purchaseRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseDTO> getPurchasesByWarehouse(Long warehouseId, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        return purchaseRepository.findByWarehouseOrderByPurchaseDateDesc(warehouse, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseDTO> getPurchasesBySupplier(Long supplierId, Pageable pageable) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return purchaseRepository.findBySupplierOrderByPurchaseDateDesc(supplier, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public PurchaseDTO getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + id));
        return convertToDTO(purchase);
    }

    @Transactional
    public PurchaseDTO createPurchase(PurchaseDTO dto, Long currentUserId) {
        // Validate
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        User user = userService.getUserEntityById(currentUserId);

        // Generate purchase number
        String purchaseNumber = generatePurchaseNumber();

        // Calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseItemDTO itemDto : dto.getItems()) {
            totalAmount = totalAmount.add(itemDto.getAmount());
        }

        // Determine payment status
        PaymentStatus paymentStatus;
        BigDecimal paidAmount = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = PaymentStatus.PENDING;
        } else if (paidAmount.compareTo(totalAmount) >= 0) {
            paymentStatus = PaymentStatus.PAID;
        } else {
            paymentStatus = PaymentStatus.PARTIAL;
        }

        // Create purchase
        Purchase purchase = Purchase.builder()
                .purchaseNumber(purchaseNumber)
                .purchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now())
                .supplier(supplier)
                .warehouse(warehouse)
                .supplierInvoiceNumber(dto.getSupplierInvoiceNumber())
                .vehicleNumber(dto.getVehicleNumber())
                .driverName(dto.getDriverName())
                .driverPhone(dto.getDriverPhone())
                .grossWeight(dto.getGrossWeight())
                .tareWeight(dto.getTareWeight())
                .netWeight(dto.getNetWeight())
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .paymentStatus(paymentStatus)
                .paymentMethod(dto.getPaymentMethod())
                .notes(dto.getNotes())
                .status(PurchaseStatus.COMPLETED) // Always set to COMPLETED (required for stock tracking)
                .createdBy(user)
                .build();

        purchase = purchaseRepository.save(purchase);

        // Create purchase items and update stock
        for (PurchaseItemDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            PurchaseItem item = PurchaseItem.builder()
                    .purchase(purchase)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unit(product.getUnit()) // Inherit unit from product
                    .rate(itemDto.getRate())
                    .amount(itemDto.getAmount())
                    .grossWeight(itemDto.getGrossWeight())
                    .tareWeight(itemDto.getTareWeight())
                    .netWeight(itemDto.getNetWeight())
                    .build();

            purchaseItemRepository.save(item);

            // Update stock - PURCHASE movement (positive quantity)
            // All purchases automatically update stock
            StockMovementDTO stockMovement = StockMovementDTO.builder()
                    .warehouseId(warehouse.getId())
                    .productId(product.getId())
                    .movementType(MovementType.PURCHASE)
                    .quantity(itemDto.getQuantity())
                    .rate(itemDto.getRate())
                    .referenceType("purchase")
                    .referenceId(purchase.getId())
                    .notes("Purchase #" + purchase.getPurchaseNumber())
                    .build();

            stockService.recordMovement(stockMovement, currentUserId);
        }

        // Update supplier ledger (Debit entry - increases payable)
        // All purchases automatically update supplier ledger
        supplierService.updateSupplierLedger(supplier, totalAmount, "DEBIT",
                "purchase", purchase.getId(), currentUserId);

        // If payment made, create credit entry
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            supplierService.updateSupplierLedger(supplier, paidAmount, "CREDIT",
                    "purchase_payment", purchase.getId(), currentUserId);
        }

        // Audit log
        createAuditLog(currentUserId, "purchases", purchase.getId(), AuditAction.INSERT,
                null, mapPurchaseToAudit(purchase));

        // Purchase-specific audit trail
        purchaseAuditService.logPurchaseCreation(purchase.getId());

        log.info("Purchase created: {} for warehouse: {} by user: {}",
                purchase.getPurchaseNumber(), warehouse.getName(), currentUserId);

        return convertToDTO(purchase);
    }

    @Transactional
    public PurchaseDTO updatePurchase(Long id, PurchaseDTO dto, Long currentUserId) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + id));

        Purchase oldPurchase = clonePurchase(purchase);
        PurchaseStatus oldStatus = purchase.getStatus();

        // Track what changed for audit
        StringBuilder changeNotes = new StringBuilder("Updated fields: ");
        boolean hasChanges = false;

        // Update basic fields and track changes
        if (!purchase.getPurchaseDate().equals(dto.getPurchaseDate())) {
            purchase.setPurchaseDate(dto.getPurchaseDate());
            changeNotes.append("Purchase Date, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getSupplierInvoiceNumber(), dto.getSupplierInvoiceNumber())) {
            purchase.setSupplierInvoiceNumber(dto.getSupplierInvoiceNumber());
            changeNotes.append("Invoice Number, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getVehicleNumber(), dto.getVehicleNumber())) {
            purchase.setVehicleNumber(dto.getVehicleNumber());
            changeNotes.append("Vehicle Number, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getDriverName(), dto.getDriverName())) {
            purchase.setDriverName(dto.getDriverName());
            changeNotes.append("Driver Name, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getDriverPhone(), dto.getDriverPhone())) {
            purchase.setDriverPhone(dto.getDriverPhone());
            changeNotes.append("Driver Phone, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getGrossWeight(), dto.getGrossWeight())) {
            purchase.setGrossWeight(dto.getGrossWeight());
            changeNotes.append("Gross Weight, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getTareWeight(), dto.getTareWeight())) {
            purchase.setTareWeight(dto.getTareWeight());
            changeNotes.append("Tare Weight, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getNetWeight(), dto.getNetWeight())) {
            purchase.setNetWeight(dto.getNetWeight());
            changeNotes.append("Net Weight, ");
            hasChanges = true;
        }
        if (!equalsNullSafe(purchase.getNotes(), dto.getNotes())) {
            purchase.setNotes(dto.getNotes());
            changeNotes.append("Notes, ");
            hasChanges = true;
        }

        purchase = purchaseRepository.save(purchase);

        // Audit log
        createAuditLog(currentUserId, "purchases", purchase.getId(), AuditAction.UPDATE,
                mapPurchaseToAudit(oldPurchase), mapPurchaseToAudit(purchase));

        // Purchase-specific audit trail
        if (hasChanges) {
            String notes = changeNotes.substring(0, changeNotes.length() - 2); // Remove trailing comma
            purchaseAuditService.logPurchaseUpdate(purchase.getId(), notes);
        }

        // Log status change if applicable
        if (oldStatus != purchase.getStatus()) {
            purchaseAuditService.logStatusChange(purchase.getId(),
                oldStatus != null ? oldStatus.name() : null,
                purchase.getStatus() != null ? purchase.getStatus().name() : null);
        }

        log.info("Purchase updated: {} by user: {}", purchase.getPurchaseNumber(), currentUserId);
        return convertToDTO(purchase);
    }

    private boolean equalsNullSafe(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }

    @Transactional
    public void deletePurchase(Long id, Long currentUserId) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + id));

        // Note: Removed restriction on deleting completed purchases
        // All deletions are now allowed but require confirmation from the user
        // This will also delete related stock movements, payments, and attachments (cascade delete)

        purchaseRepository.delete(purchase);

        // Audit log
        createAuditLog(currentUserId, "purchases", id, AuditAction.DELETE,
                mapPurchaseToAudit(purchase), null);

        log.info("Purchase deleted: {} by user: {}", purchase.getPurchaseNumber(), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> getPurchasesByDateRange(Long warehouseId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        return purchaseRepository.findByWarehouseAndDateRange(warehouse, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseDTO> getSupplierPurchasesByDateRange(Long supplierId,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        return purchaseRepository.findBySupplierAndDateRange(supplier, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generatePurchaseNumber() {
        String prefix = "PUR-";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = purchaseRepository.countPurchasesFromDate(LocalDate.now()) + 1;
        return String.format("%s%s-%04d", prefix, datePart, count);
    }

    private PurchaseDTO convertToDTO(Purchase purchase) {
        List<PurchaseItemDTO> itemDTOs = purchaseItemRepository.findByPurchase(purchase)
                .stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());

        return PurchaseDTO.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .purchaseDate(purchase.getPurchaseDate())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getName())
                .supplierCode(purchase.getSupplier().getCode())
                .warehouseId(purchase.getWarehouse().getId())
                .warehouseName(purchase.getWarehouse().getName())
                .supplierInvoiceNumber(purchase.getSupplierInvoiceNumber())
                .vehicleNumber(purchase.getVehicleNumber())
                .driverName(purchase.getDriverName())
                .driverPhone(purchase.getDriverPhone())
                .grossWeight(purchase.getGrossWeight())
                .tareWeight(purchase.getTareWeight())
                .netWeight(purchase.getNetWeight())
                .totalAmount(purchase.getTotalAmount())
                .paidAmount(purchase.getPaidAmount())
                .paymentStatus(purchase.getPaymentStatus())
                .paymentMethod(purchase.getPaymentMethod())
                .notes(purchase.getNotes())
                .status(purchase.getStatus())
                .createdBy(purchase.getCreatedBy() != null ? purchase.getCreatedBy().getId() : null)
                .createdByName(purchase.getCreatedBy() != null ? purchase.getCreatedBy().getName() : null)
                .items(itemDTOs)
                .build();
    }

    private PurchaseItemDTO convertItemToDTO(PurchaseItem item) {
        // Use unit from item if available, otherwise fallback to product's unit
        String unitDisplay = item.getUnit() != null
                ? item.getUnit().getDisplayName()
                : item.getProduct().getUnit().getDisplayName();

        return PurchaseItemDTO.builder()
                .id(item.getId())
                .purchaseId(item.getPurchase().getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .productUnit(unitDisplay)
                .quantity(item.getQuantity())
                .rate(item.getRate())
                .amount(item.getAmount())
                .grossWeight(item.getGrossWeight())
                .tareWeight(item.getTareWeight())
                .netWeight(item.getNetWeight())
                .build();
    }

    private Purchase clonePurchase(Purchase purchase) {
        return Purchase.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .purchaseDate(purchase.getPurchaseDate())
                .supplier(purchase.getSupplier())
                .warehouse(purchase.getWarehouse())
                .supplierInvoiceNumber(purchase.getSupplierInvoiceNumber())
                .vehicleNumber(purchase.getVehicleNumber())
                .driverName(purchase.getDriverName())
                .driverPhone(purchase.getDriverPhone())
                .totalAmount(purchase.getTotalAmount())
                .paidAmount(purchase.getPaidAmount())
                .paymentStatus(purchase.getPaymentStatus())
                .status(purchase.getStatus())
                .build();
    }

    private void createAuditLog(Long userId, String tableName, Long recordId, AuditAction action,
                               Map<String, Object> oldValue, Map<String, Object> newValue) {
        User currentUser = userService.getUserEntityById(userId);

        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(currentUser != null ? currentUser.getId() : userId);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);

        auditLogRepository.save(auditLog);
    }

    private Map<String, Object> mapPurchaseToAudit(Purchase purchase) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", purchase.getId());
        map.put("purchaseNumber", purchase.getPurchaseNumber());
        map.put("purchaseDate", purchase.getPurchaseDate());
        map.put("supplierId", purchase.getSupplier().getId());
        map.put("warehouseId", purchase.getWarehouse().getId());
        map.put("totalAmount", purchase.getTotalAmount());
        map.put("status", purchase.getStatus() != null ? purchase.getStatus().name() : null);
        return map;
    }
}