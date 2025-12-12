package com.warehouse.wms.service;

import com.warehouse.wms.dto.PurchaseReportDTO;
import com.warehouse.wms.dto.ReportSummaryDTO;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.repository.PurchaseRepository;
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
public class PurchaseReportService {

    private final PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public List<PurchaseReportDTO> getPurchaseReport(Long warehouseId, Long supplierId,
                                                     LocalDate startDate, LocalDate endDate) {
        List<Purchase> purchases = purchaseRepository.findWithFilters(
                warehouseId, supplierId, null, startDate, endDate);

        return purchases.stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportSummaryDTO getPurchaseSummary(Long warehouseId, Long supplierId,
                                               LocalDate startDate, LocalDate endDate) {
        List<Purchase> purchases = purchaseRepository.findWithFilters(
                warehouseId, supplierId, null, startDate, endDate);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;

        for (Purchase purchase : purchases) {
            totalAmount = totalAmount.add(purchase.getTotalAmount());
            paidAmount = paidAmount.add(purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO);
        }

        BigDecimal outstandingAmount = totalAmount.subtract(paidAmount);

        return ReportSummaryDTO.builder()
                .totalCount((long) purchases.size())
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .outstandingAmount(outstandingAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PurchaseReportDTO> getPurchasesBySupplier(LocalDate startDate, LocalDate endDate) {
        List<Purchase> purchases = purchaseRepository.findWithFilters(null, null, null, startDate, endDate);
        return purchases.stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseReportDTO> getPurchasesByWarehouse(LocalDate startDate, LocalDate endDate) {
        List<Purchase> purchases = purchaseRepository.findWithFilters(null, null, null, startDate, endDate);
        return purchases.stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    private PurchaseReportDTO convertToReportDTO(Purchase purchase) {
        BigDecimal paidAmount = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstandingAmount = purchase.getTotalAmount().subtract(paidAmount);

        return PurchaseReportDTO.builder()
                .purchaseDate(purchase.getPurchaseDate())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierName(purchase.getSupplier().getName())
                .supplierCode(purchase.getSupplier().getCode())
                .warehouseName(purchase.getWarehouse().getName())
                .totalAmount(purchase.getTotalAmount())
                .paidAmount(paidAmount)
                .outstandingAmount(outstandingAmount)
                .paymentStatus(purchase.getPaymentStatus() != null ? purchase.getPaymentStatus().name() : "PENDING")
                .paymentMethod(purchase.getPaymentMethod() != null ? purchase.getPaymentMethod().name() : null)
                .itemCount(purchase.getItems() != null ? purchase.getItems().size() : 0)
                .build();
    }
}
