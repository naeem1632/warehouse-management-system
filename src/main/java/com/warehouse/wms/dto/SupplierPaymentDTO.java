package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentDTO {
    private Long id;
    private String paymentNumber;
    private LocalDate paymentDate;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private BigDecimal amount;
    private String paymentMethod;
    private String bankName;
    private String accountNumber;
    private String transactionReference;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
