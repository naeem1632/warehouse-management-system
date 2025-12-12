package com.warehouse.wms.enums;

public enum AdjustmentStatus {
    PENDING,    // Waiting for approval
    APPROVED,   // Approved and applied to stock
    REJECTED    // Rejected, not applied
}
