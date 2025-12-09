package com.warehouse.wms.service;

import com.warehouse.wms.dto.SupplierDTO;
import com.warehouse.wms.dto.SupplierLedgerDTO;
import com.warehouse.wms.entity.AuditLog;
import com.warehouse.wms.entity.Supplier;
import com.warehouse.wms.entity.SupplierLedger;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.enums.AuditAction;
import com.warehouse.wms.enums.BalanceType;
import com.warehouse.wms.repository.AuditLogRepository;
import com.warehouse.wms.repository.SupplierLedgerRepository;
import com.warehouse.wms.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierLedgerRepository ledgerRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDTO> getActiveSuppliers(Pageable pageable) {
        return supplierRepository.findByStatus("ACTIVE", pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> getAllActiveSuppliers() {
        return supplierRepository.findByStatus("ACTIVE")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        return convertToDTO(supplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDTO> searchSuppliers(String keyword, Pageable pageable) {
        return supplierRepository.searchSuppliers(keyword, pageable).map(this::convertToDTO);
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto, Long currentUserId) {
        // Validate code uniqueness
        if (supplierRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Supplier code already exists: " + dto.getCode());
        }

        Supplier supplier = Supplier.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .businessType(dto.getBusinessType())
                .contactPerson(dto.getContactPerson())
                .designation(dto.getDesignation())
                .phone(dto.getPhone())
                .alternatePhone(dto.getAlternatePhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .city(dto.getCity())
                .ntn(dto.getNtn())
                .strn(dto.getStrn())
                .paymentTerms(dto.getPaymentTerms())
                .creditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO)
                .openingBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO)
                .openingBalanceType(dto.getOpeningBalanceType())
                .openingDate(dto.getOpeningDate())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        supplier = supplierRepository.save(supplier);

        // Create opening balance entry if provided
        if (dto.getOpeningBalance() != null &&
            dto.getOpeningBalance().compareTo(BigDecimal.ZERO) != 0) {
            createOpeningBalanceEntry(supplier, currentUserId);
        }

        // Audit log
        createAuditLog(currentUserId, "suppliers", supplier.getId(), AuditAction.INSERT, null, mapSupplierToAudit(supplier));

        log.info("Supplier created: {} by user: {}", supplier.getCode(), currentUserId);
        return convertToDTO(supplier);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto, Long currentUserId) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));

        Supplier oldSupplier = cloneSupplier(supplier);

        // Validate code uniqueness
        if (!supplier.getCode().equals(dto.getCode()) &&
            supplierRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Supplier code already exists: " + dto.getCode());
        }

        supplier.setCode(dto.getCode());
        supplier.setName(dto.getName());
        supplier.setBusinessType(dto.getBusinessType());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setDesignation(dto.getDesignation());
        supplier.setPhone(dto.getPhone());
        supplier.setAlternatePhone(dto.getAlternatePhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setCity(dto.getCity());
        supplier.setNtn(dto.getNtn());
        supplier.setStrn(dto.getStrn());
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setCreditLimit(dto.getCreditLimit());
        supplier.setStatus(dto.getStatus());

        supplier = supplierRepository.save(supplier);

        // Audit log
        createAuditLog(currentUserId, "suppliers", supplier.getId(), AuditAction.UPDATE,
                mapSupplierToAudit(oldSupplier), mapSupplierToAudit(supplier));

        log.info("Supplier updated: {} by user: {}", supplier.getCode(), currentUserId);
        return convertToDTO(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id, Long currentUserId) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));

        // Check if supplier has any transactions
        List<SupplierLedger> ledgerEntries = ledgerRepository.findBySupplierOrderByTransactionDateAsc(supplier);
        if (!ledgerEntries.isEmpty() && ledgerEntries.size() > 1) { // More than just opening balance
            throw new RuntimeException("Cannot delete supplier with transaction history");
        }

        supplierRepository.delete(supplier);

        // Audit log
        createAuditLog(currentUserId, "suppliers", id, AuditAction.DELETE,
                mapSupplierToAudit(supplier), null);

        log.info("Supplier deleted: {} by user: {}", supplier.getCode(), currentUserId);
    }

    @Transactional
    public void createOpeningBalanceEntry(Supplier supplier, Long currentUserId) {
        User user = userService.getUserEntityById(currentUserId);

        BigDecimal balance = supplier.getOpeningBalance();
        BalanceType balanceType = supplier.getOpeningBalanceType();

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        if (balanceType == BalanceType.DEBIT) {
            debit = balance;
        } else {
            credit = balance;
        }

        SupplierLedger ledger = SupplierLedger.builder()
                .supplier(supplier)
                .transactionDate(supplier.getOpeningDate() != null ?
                        supplier.getOpeningDate() : LocalDate.now())
                .description("Opening Balance")
                .referenceType("opening_balance")
                .referenceId(supplier.getId())
                .debit(debit)
                .credit(credit)
                .balance(balance)
                .balanceType(balanceType)
                .createdBy(user)
                .build();

        ledgerRepository.save(ledger);
        log.info("Opening balance created for supplier: {}", supplier.getCode());
    }

    @Transactional
    public void updateSupplierLedger(Supplier supplier, BigDecimal amount,
                                     String type, String referenceType,
                                     Long referenceId, Long currentUserId) {
        User user = userService.getUserEntityById(currentUserId);

        // Get latest balance
        SupplierLedger latestLedger = ledgerRepository.findLatestBySupplier(supplier).orElse(null);

        BigDecimal currentBalance = BigDecimal.ZERO;
        BalanceType currentBalanceType = BalanceType.DEBIT;

        if (latestLedger != null) {
            currentBalance = latestLedger.getBalance();
            currentBalanceType = latestLedger.getBalanceType();
        }

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        // Calculate new balance
        BigDecimal newBalance;
        BalanceType newBalanceType;

        if (type.equals("DEBIT")) {
            debit = amount;
            // Adding debit
            if (currentBalanceType == BalanceType.DEBIT) {
                newBalance = currentBalance.add(amount);
                newBalanceType = BalanceType.DEBIT;
            } else {
                // Current is credit, debit reduces it
                if (amount.compareTo(currentBalance) > 0) {
                    newBalance = amount.subtract(currentBalance);
                    newBalanceType = BalanceType.DEBIT;
                } else {
                    newBalance = currentBalance.subtract(amount);
                    newBalanceType = BalanceType.CREDIT;
                }
            }
        } else {
            credit = amount;
            // Adding credit
            if (currentBalanceType == BalanceType.CREDIT) {
                newBalance = currentBalance.add(amount);
                newBalanceType = BalanceType.CREDIT;
            } else {
                // Current is debit, credit reduces it
                if (amount.compareTo(currentBalance) > 0) {
                    newBalance = amount.subtract(currentBalance);
                    newBalanceType = BalanceType.CREDIT;
                } else {
                    newBalance = currentBalance.subtract(amount);
                    newBalanceType = BalanceType.DEBIT;
                }
            }
        }

        SupplierLedger ledger = SupplierLedger.builder()
                .supplier(supplier)
                .transactionDate(LocalDate.now())
                .description(referenceType.replace("_", " ").toUpperCase())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .debit(debit)
                .credit(credit)
                .balance(newBalance)
                .balanceType(newBalanceType)
                .createdBy(user)
                .build();

        ledgerRepository.save(ledger);
        log.info("Supplier ledger updated for: {}, Amount: {}, Type: {}",
                supplier.getCode(), amount, type);
    }

    @Transactional(readOnly = true)
    public List<SupplierLedgerDTO> getSupplierLedger(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        return ledgerRepository.findBySupplierOrderByTransactionDateAsc(supplier)
                .stream()
                .map(this::convertLedgerToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SupplierLedgerDTO> getSupplierLedgerPaginated(Long supplierId, Pageable pageable) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        return ledgerRepository.findBySupplierOrderByTransactionDateDesc(supplier, pageable)
                .map(this::convertLedgerToDTO);
    }

    @Transactional(readOnly = true)
    public List<SupplierLedgerDTO> getSupplierLedgerByDateRange(Long supplierId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        return ledgerRepository.findBySupplierAndDateRange(supplier, startDate, endDate)
                .stream()
                .map(this::convertLedgerToDTO)
                .collect(Collectors.toList());
    }

    private SupplierDTO convertToDTO(Supplier supplier) {
        // Get current balance from latest ledger entry
        SupplierLedger latestLedger = ledgerRepository.findLatestBySupplier(supplier).orElse(null);

        return SupplierDTO.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .businessType(supplier.getBusinessType())
                .contactPerson(supplier.getContactPerson())
                .designation(supplier.getDesignation())
                .phone(supplier.getPhone())
                .alternatePhone(supplier.getAlternatePhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .ntn(supplier.getNtn())
                .strn(supplier.getStrn())
                .paymentTerms(supplier.getPaymentTerms())
                .creditLimit(supplier.getCreditLimit())
                .openingBalance(supplier.getOpeningBalance())
                .openingBalanceType(supplier.getOpeningBalanceType())
                .openingDate(supplier.getOpeningDate())
                .status(supplier.getStatus())
                .currentBalance(latestLedger != null ? latestLedger.getBalance() : BigDecimal.ZERO)
                .currentBalanceType(latestLedger != null ? latestLedger.getBalanceType() : BalanceType.DEBIT)
                .build();
    }

    private SupplierLedgerDTO convertLedgerToDTO(SupplierLedger ledger) {
        return SupplierLedgerDTO.builder()
                .id(ledger.getId())
                .supplierId(ledger.getSupplier().getId())
                .supplierName(ledger.getSupplier().getName())
                .transactionDate(ledger.getTransactionDate())
                .description(ledger.getDescription())
                .referenceType(ledger.getReferenceType())
                .referenceId(ledger.getReferenceId())
                .debit(ledger.getDebit())
                .credit(ledger.getCredit())
                .balance(ledger.getBalance())
                .balanceType(ledger.getBalanceType())
                .createdBy(ledger.getCreatedBy() != null ? ledger.getCreatedBy().getId() : null)
                .createdByName(ledger.getCreatedBy() != null ? ledger.getCreatedBy().getName() : null)
                .build();
    }

    private Supplier cloneSupplier(Supplier supplier) {
        return Supplier.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .businessType(supplier.getBusinessType())
                .contactPerson(supplier.getContactPerson())
                .designation(supplier.getDesignation())
                .phone(supplier.getPhone())
                .alternatePhone(supplier.getAlternatePhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .ntn(supplier.getNtn())
                .strn(supplier.getStrn())
                .paymentTerms(supplier.getPaymentTerms())
                .creditLimit(supplier.getCreditLimit())
                .status(supplier.getStatus())
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

    private Map<String, Object> mapSupplierToAudit(Supplier supplier) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", supplier.getId());
        map.put("code", supplier.getCode());
        map.put("name", supplier.getName());
        map.put("businessType", supplier.getBusinessType() != null ? supplier.getBusinessType().name() : null);
        map.put("contactPerson", supplier.getContactPerson());
        map.put("phone", supplier.getPhone());
        map.put("email", supplier.getEmail());
        map.put("status", supplier.getStatus());
        return map;
    }
}