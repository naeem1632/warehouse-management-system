package com.warehouse.wms.service;

import com.warehouse.wms.dto.SupplierPaymentDTO;
import com.warehouse.wms.entity.Supplier;
import com.warehouse.wms.entity.SupplierPayment;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.repository.SupplierPaymentRepository;
import com.warehouse.wms.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentService {

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierService supplierService;
    private final UserService userService;

    @Transactional
    public SupplierPaymentDTO createPayment(SupplierPaymentDTO dto, Long currentUserId) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        User user = userService.getUserEntityById(currentUserId);

        // Generate payment number
        String paymentNumber = generatePaymentNumber();

        SupplierPayment payment = SupplierPayment.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(dto.getPaymentDate())
                .supplier(supplier)
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .transactionReference(dto.getTransactionReference())
                .chequeNumber(dto.getChequeNumber())
                .chequeDate(dto.getChequeDate())
                .notes(dto.getNotes())
                .createdBy(user)
                .build();

        payment = paymentRepository.save(payment);

        // Update supplier ledger - CREDIT entry (payment reduces payable)
        supplierService.updateSupplierLedger(supplier, dto.getAmount(), "CREDIT",
                "payment", payment.getId(), currentUserId);

        log.info("Supplier payment created: {} for supplier: {} by user: {}",
                paymentNumber, supplier.getCode(), currentUserId);

        return convertToDTO(payment);
    }

    @Transactional(readOnly = true)
    public SupplierPaymentDTO getPaymentById(Long id) {
        SupplierPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return convertToDTO(payment);
    }

    @Transactional(readOnly = true)
    public Page<SupplierPaymentDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllByOrderByPaymentDateDesc(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SupplierPaymentDTO> getPaymentsWithFilters(Long supplierId, LocalDate startDate,
                                                           LocalDate endDate, Pageable pageable) {
        return paymentRepository.findWithFilters(supplierId, startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentDTO> getPaymentsBySupplier(Long supplierId) {
        return paymentRepository.findBySupplierIdOrderByPaymentDateDesc(supplierId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generatePaymentNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SPY-" + today + "-";

        SupplierPayment latestPayment = paymentRepository.findLatestByPaymentNumberPrefix(prefix)
                .orElse(null);

        int nextSequence = 1;
        if (latestPayment != null) {
            String lastNumber = latestPayment.getPaymentNumber();
            String sequencePart = lastNumber.substring(lastNumber.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        return prefix + String.format("%04d", nextSequence);
    }

    private SupplierPaymentDTO convertToDTO(SupplierPayment payment) {
        return SupplierPaymentDTO.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .paymentDate(payment.getPaymentDate())
                .supplierId(payment.getSupplier().getId())
                .supplierName(payment.getSupplier().getName())
                .supplierCode(payment.getSupplier().getCode())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .bankName(payment.getBankName())
                .accountNumber(payment.getAccountNumber())
                .transactionReference(payment.getTransactionReference())
                .chequeNumber(payment.getChequeNumber())
                .chequeDate(payment.getChequeDate())
                .notes(payment.getNotes())
                .createdBy(payment.getCreatedBy() != null ? payment.getCreatedBy().getId() : null)
                .createdByName(payment.getCreatedBy() != null ? payment.getCreatedBy().getName() : null)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
