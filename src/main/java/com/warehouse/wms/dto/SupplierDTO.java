package com.warehouse.wms.dto;

import com.warehouse.wms.enums.BalanceType;
import com.warehouse.wms.enums.BusinessType;
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
public class SupplierDTO {
    private Long id;
    private String code;
    private String name;
    private BusinessType businessType;
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
    private BalanceType openingBalanceType;
    private LocalDate openingDate;
    private String status;

    // For display purposes
    private BigDecimal currentBalance;
    private BalanceType currentBalanceType;
}