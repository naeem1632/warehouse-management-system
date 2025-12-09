package com.warehouse.wms.enums;

public enum PaymentMethod {
    CASH("Cash"),
    BANK("Bank Transfer"),
    CHEQUE("Cheque"),
    CREDIT("Credit");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}