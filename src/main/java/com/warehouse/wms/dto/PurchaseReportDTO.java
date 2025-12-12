package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseReportDTO {
    private LocalDate purchaseDate;
    private String purchaseNumber;
    private String supplierName;
    private String supplierCode;
    private String warehouseName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String paymentStatus;
    private String paymentMethod;
    private Integer itemCount;
}
