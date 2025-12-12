package com.warehouse.wms.service;

import com.warehouse.wms.dto.ReportSummaryDTO;
import com.warehouse.wms.dto.SalesReportDTO;
import com.warehouse.wms.entity.Sale;
import com.warehouse.wms.repository.SaleItemRepository;
import com.warehouse.wms.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesReportService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    @Transactional(readOnly = true)
    public List<SalesReportDTO> getSalesReport(Long warehouseId, Long customerId,
                                               LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findWithFilters(warehouseId, customerId, startDate, endDate);

        return sales.stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportSummaryDTO getSalesSummary(Long warehouseId, Long customerId,
                                            LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findWithFilters(warehouseId, customerId, startDate, endDate);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (Sale sale : sales) {
            totalAmount = totalAmount.add(sale.getTotalAmount());
            receivedAmount = receivedAmount.add(sale.getReceivedAmount() != null ? sale.getReceivedAmount() : BigDecimal.ZERO);
            discountAmount = discountAmount.add(sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO);
        }

        BigDecimal outstandingAmount = totalAmount.subtract(receivedAmount);

        return ReportSummaryDTO.builder()
                .totalCount((long) sales.size())
                .totalAmount(totalAmount)
                .paidAmount(receivedAmount)
                .outstandingAmount(outstandingAmount)
                .build();
    }

    private SalesReportDTO convertToReportDTO(Sale sale) {
        BigDecimal receivedAmount = sale.getReceivedAmount() != null ? sale.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal outstandingAmount = sale.getTotalAmount().subtract(receivedAmount);

        // Get item count from repository
        int itemCount = saleItemRepository.findBySale(sale).size();

        return SalesReportDTO.builder()
                .saleDate(sale.getSaleDate())
                .saleNumber(sale.getSaleNumber())
                .invoiceNumber(sale.getInvoiceNumber())
                .customerName(sale.getCustomer().getName())
                .customerCode(sale.getCustomer().getCode())
                .warehouseName(sale.getWarehouse().getName())
                .subtotal(sale.getSubtotal())
                .discount(sale.getDiscount())
                .totalAmount(sale.getTotalAmount())
                .receivedAmount(receivedAmount)
                .outstandingAmount(outstandingAmount)
                .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : "PENDING")
                .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null)
                .itemCount(itemCount)
                .build();
    }
}
