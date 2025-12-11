package com.warehouse.wms.repository;

import com.warehouse.wms.entity.PurchaseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseAuditLogRepository extends JpaRepository<PurchaseAuditLog, Long> {

    List<PurchaseAuditLog> findByPurchaseIdOrderByChangedAtDesc(Long purchaseId);
}
