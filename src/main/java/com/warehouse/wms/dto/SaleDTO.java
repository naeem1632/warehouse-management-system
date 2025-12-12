package com.warehouse.wms.dto;

import com.warehouse.wms.enums.PaymentMethod;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.enums.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleDTO {
    private Long id;
    private String saleNumber;
    private String invoiceNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate saleDate;

    private Long customerId;
    private String customerName;
    private String customerCode;

    private Long warehouseId;
    private String warehouseName;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;

    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private String notes;
    private SaleStatus status;

    private Long createdBy;
    private String createdByName;

    @Builder.Default
    private List<SaleItemDTO> items = new ArrayList<>();
}
