package com.warehouse.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseAuditLogDTO {
    private Long id;
    private Long purchaseId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long changedBy;
    private String changedByName;
    private LocalDateTime changedAt;
    private String notes;
}
