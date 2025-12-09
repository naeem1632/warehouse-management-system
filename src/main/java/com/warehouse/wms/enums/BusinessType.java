package com.warehouse.wms.enums;

public enum BusinessType {
    MANUFACTURER("Manufacturer"),
    DISTRIBUTOR("Distributor"),
    WHOLESALER("Wholesaler");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}