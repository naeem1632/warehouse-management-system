package com.warehouse.wms.enums;

public enum PaymentStatus {
    PAID("Paid"),
    PARTIAL("Partial"),
    PENDING("Pending");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}