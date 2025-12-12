package com.warehouse.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLedgerDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private LocalDate transactionDate;
    private String description;
    private String referenceType;
    private Long referenceId;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private String balanceType;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
