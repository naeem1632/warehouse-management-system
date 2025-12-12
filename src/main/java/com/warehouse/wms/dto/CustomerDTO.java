package com.warehouse.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String code;
    private String name;
    private String businessType;
    private String contactPerson;
    private String designation;
    private String phone;
    private String alternatePhone;
    private String email;
    private String address;
    private String city;
    private String ntn;
    private String strn;
    private String paymentTerms;
    private BigDecimal creditLimit;
    private BigDecimal openingBalance;
    private String openingBalanceType; // DEBIT or CREDIT

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate openingDate;
    private String status;

    // Calculated field - current balance from ledger
    private BigDecimal currentBalance;
    private String currentBalanceType;
}
