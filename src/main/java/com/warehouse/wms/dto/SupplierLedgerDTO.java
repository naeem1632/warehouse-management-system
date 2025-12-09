package com.warehouse.wms.dto;

import com.warehouse.wms.enums.BalanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierLedgerDTO {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private LocalDate transactionDate;
    private String description;
    private String referenceType;
    private Long referenceId;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private BalanceType balanceType;
    private Long createdBy;
    private String createdByName;
}