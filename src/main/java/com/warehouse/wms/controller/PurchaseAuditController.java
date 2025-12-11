package com.warehouse.wms.controller;

import com.warehouse.wms.dto.PurchaseAuditLogDTO;
import com.warehouse.wms.service.PurchaseAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseAuditController {

    private final PurchaseAuditService purchaseAuditService;

    @GetMapping("/{purchaseId}/audit-logs")
    @ResponseBody
    public ResponseEntity<List<PurchaseAuditLogDTO>> getAuditLogs(@PathVariable Long purchaseId) {
        List<PurchaseAuditLogDTO> logs = purchaseAuditService.getAuditLogsByPurchaseId(purchaseId);
        return ResponseEntity.ok(logs);
    }
}
