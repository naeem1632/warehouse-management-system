package com.warehouse.wms.service;

import com.warehouse.wms.dto.PurchaseAuditLogDTO;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.PurchaseAuditLog;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.repository.PurchaseAuditLogRepository;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseAuditService {

    private final PurchaseAuditLogRepository auditLogRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public List<PurchaseAuditLogDTO> getAuditLogsByPurchaseId(Long purchaseId) {
        System.out.println("=== Getting audit logs for purchase: " + purchaseId);
        List<PurchaseAuditLog> logs = auditLogRepository.findByPurchaseIdOrderByChangedAtDesc(purchaseId);
        System.out.println("Found " + logs.size() + " audit logs");

        List<PurchaseAuditLogDTO> dtos = logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        System.out.println("Returning " + dtos.size() + " audit log DTOs");
        return dtos;
    }

    public void logPurchaseChange(Long purchaseId, String action, String fieldName, String oldValue, String newValue, String notes) {
        System.out.println("=== Logging purchase change for purchase: " + purchaseId);
        System.out.println("Action: " + action + ", Notes: " + notes);

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        System.out.println("Current username: " + currentUsername);

        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        System.out.println("Current user: " + (currentUser != null ? currentUser.getId() : "null"));

        PurchaseAuditLog log = PurchaseAuditLog.builder()
                .purchase(purchase)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(currentUser)
                .changedAt(LocalDateTime.now())
                .notes(notes)
                .build();

        PurchaseAuditLog saved = auditLogRepository.save(log);
        System.out.println("Audit log saved with ID: " + saved.getId());
    }

    public void logPurchaseCreation(Long purchaseId) {
        System.out.println("=== Logging purchase CREATION for purchase: " + purchaseId);
        logPurchaseChange(purchaseId, "CREATED", null, null, null, "Purchase created");
    }

    public void logPurchaseUpdate(Long purchaseId, String notes) {
        System.out.println("=== Logging purchase UPDATE for purchase: " + purchaseId);
        logPurchaseChange(purchaseId, "UPDATED", null, null, null, notes);
    }

    public void logStatusChange(Long purchaseId, String oldStatus, String newStatus) {
        logPurchaseChange(purchaseId, "STATUS_CHANGED", "status", oldStatus, newStatus, "Purchase status changed");
    }

    private PurchaseAuditLogDTO convertToDTO(PurchaseAuditLog log) {
        return PurchaseAuditLogDTO.builder()
                .id(log.getId())
                .purchaseId(log.getPurchase().getId())
                .action(log.getAction())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .changedBy(log.getChangedBy() != null ? log.getChangedBy().getId() : null)
                .changedByName(log.getChangedBy() != null ? log.getChangedBy().getName() : null)
                .changedAt(log.getChangedAt())
                .notes(log.getNotes())
                .build();
    }
}
