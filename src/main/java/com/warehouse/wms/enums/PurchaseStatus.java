package com.warehouse.wms.enums;

public enum PurchaseStatus {
    COMPLETED("Completed"),
    DRAFT("Draft"),
    CANCELLED("Cancelled");

    private final String displayName;

    PurchaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}