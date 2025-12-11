package com.warehouse.wms.controller;

import com.warehouse.wms.dto.PurchasePaymentDTO;
import com.warehouse.wms.service.PurchasePaymentService;
import com.warehouse.wms.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchasePaymentController {

    private final PurchasePaymentService purchasePaymentService;

    @GetMapping("/{purchaseId}/payments")
    @ResponseBody
    public ResponseEntity<List<PurchasePaymentDTO>> getPayments(@PathVariable Long purchaseId) {
        List<PurchasePaymentDTO> payments = purchasePaymentService.getPaymentsByPurchaseId(purchaseId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{purchaseId}/payments")
    @ResponseBody
    public ResponseEntity<PurchasePaymentDTO> addPayment(
            @PathVariable Long purchaseId,
            @Valid @RequestBody PurchasePaymentDTO paymentDTO) {

        paymentDTO.setPurchaseId(purchaseId);
        PurchasePaymentDTO saved = purchasePaymentService.addPayment(paymentDTO);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/payments/{paymentId}")
    @ResponseBody
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        purchasePaymentService.deletePayment(paymentId);
        return ResponseEntity.ok().build();
    }
}
