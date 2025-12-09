package com.warehouse.wms.enums;

public enum MovementType {
    OPENING("Opening Balance"),
    PURCHASE("Purchase"),
    SALE("Sale"),
    TRANSFER_IN("Transfer In"),
    TRANSFER_OUT("Transfer Out"),
    ADJUSTMENT("Adjustment");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
