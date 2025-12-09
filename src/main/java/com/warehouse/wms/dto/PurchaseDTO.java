package com.warehouse.wms.dto;

import com.warehouse.wms.enums.PaymentMethod;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseDTO {
    private Long id;
    private String purchaseNumber;
    private LocalDate purchaseDate;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private Long warehouseId;
    private String warehouseName;
    private String supplierInvoiceNumber;
    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    private BigDecimal netWeight;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String notes;
    private PurchaseStatus status;
    private Long createdBy;
    private String createdByName;

    @Builder.Default
    private List<PurchaseItemDTO> items = new ArrayList<>();
}