package com.warehouse.wms.service;

import com.warehouse.wms.dto.PurchasePaymentDTO;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.PurchasePayment;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.repository.PurchasePaymentRepository;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchasePaymentService {

    private final PurchasePaymentRepository purchasePaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public List<PurchasePaymentDTO> getPaymentsByPurchaseId(Long purchaseId) {
        return purchasePaymentRepository.findByPurchaseIdOrderByPaymentDateDesc(purchaseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PurchasePaymentDTO addPayment(PurchasePaymentDTO dto) {
        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + dto.getPurchaseId()));

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        PurchasePayment payment = PurchasePayment.builder()
                .purchase(purchase)
                .paymentDate(dto.getPaymentDate())
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .referenceNumber(dto.getReferenceNumber())
                .notes(dto.getNotes())
                .createdBy(currentUser)
                .build();

        PurchasePayment saved = purchasePaymentRepository.save(payment);

        // Update purchase payment status
        updatePurchasePaymentStatus(purchase);

        return convertToDTO(saved);
    }

    public void deletePayment(Long paymentId) {
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        Purchase purchase = payment.getPurchase();
        purchasePaymentRepository.delete(payment);

        // Update purchase payment status
        updatePurchasePaymentStatus(purchase);
    }

    public BigDecimal getTotalPaidAmount(Long purchaseId) {
        return purchasePaymentRepository.sumPaymentsByPurchaseId(purchaseId);
    }

    public BigDecimal getTotalPaymentsBySupplierId(Long supplierId) {
        return purchasePaymentRepository.sumPaymentsBySupplierId(supplierId);
    }

    private void updatePurchasePaymentStatus(Purchase purchase) {
        BigDecimal totalPaid = getTotalPaidAmount(purchase.getId());
        BigDecimal totalAmount = purchase.getTotalAmount();

        purchase.setPaidAmount(totalPaid);

        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setPaymentStatus(PaymentStatus.PENDING);
        } else if (totalPaid.compareTo(totalAmount) >= 0) {
            purchase.setPaymentStatus(PaymentStatus.PAID);
        } else {
            purchase.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        purchaseRepository.save(purchase);
    }

    private PurchasePaymentDTO convertToDTO(PurchasePayment payment) {
        return PurchasePaymentDTO.builder()
                .id(payment.getId())
                .purchaseId(payment.getPurchase().getId())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .createdBy(payment.getCreatedBy() != null ? payment.getCreatedBy().getId() : null)
                .createdByName(payment.getCreatedBy() != null ? payment.getCreatedBy().getName() : null)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
