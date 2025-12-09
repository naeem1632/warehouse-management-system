package com.warehouse.wms.enums;

public enum ProductUnit {
    PCS("Pieces"),
    KG("Kilogram"),
    LTR("Liter"),
    BOX("Box"),
    ROLL("Roll"),
    METER("Meter"),
    CARTON("Carton");

    private final String displayName;

    ProductUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
