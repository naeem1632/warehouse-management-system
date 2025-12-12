package com.warehouse.wms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPaymentDTO {
    private Long id;
    private String receiptNumber;
    private LocalDate paymentDate;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private BigDecimal amount;
    private String paymentMethod;
    private String bankName;
    private String accountNumber;
    private String transactionReference;
    private String chequeNumber;
    private LocalDate chequeDate;
    private String notes;
    private Long receivedBy;
    private String receivedByName;
    private LocalDateTime createdAt;
}
