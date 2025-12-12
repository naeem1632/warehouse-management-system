package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportDTO {
    private LocalDate saleDate;
    private String saleNumber;
    private String invoiceNumber;
    private String customerName;
    private String customerCode;
    private String warehouseName;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal outstandingAmount;
    private String paymentStatus;
    private String paymentMethod;
    private Integer itemCount;
}
