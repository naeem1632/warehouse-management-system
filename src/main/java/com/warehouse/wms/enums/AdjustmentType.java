package com.warehouse.wms.enums;

public enum AdjustmentType {
    PHYSICAL_COUNT,  // Physical stock count adjustment
    DAMAGE,          // Damaged goods
    FOUND,           // Found extra stock
    LOSS,            // Lost/Missing stock
    EXPIRED,         // Expired products
    OTHER            // Other reasons
}
