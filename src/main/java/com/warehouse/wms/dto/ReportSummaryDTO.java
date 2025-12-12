package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryDTO {
    private Long totalCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal profitAmount;
    private BigDecimal profitMargin;
}
