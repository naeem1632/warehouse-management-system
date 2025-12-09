package com.warehouse.wms.enums;

public enum BalanceType {
    DEBIT("Debit"),
    CREDIT("Credit");

    private final String displayName;

    BalanceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}