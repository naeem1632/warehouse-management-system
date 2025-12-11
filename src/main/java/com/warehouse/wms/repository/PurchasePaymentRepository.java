package com.warehouse.wms.repository;

import com.warehouse.wms.entity.PurchasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {

    List<PurchasePayment> findByPurchaseIdOrderByPaymentDateDesc(Long purchaseId);

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PurchasePayment pp WHERE pp.purchase.id = :purchaseId")
    BigDecimal sumPaymentsByPurchaseId(@Param("purchaseId") Long purchaseId);

    @Query("SELECT COALESCE(SUM(pp.amount), 0) FROM PurchasePayment pp WHERE pp.purchase.supplier.id = :supplierId")
    BigDecimal sumPaymentsBySupplierId(@Param("supplierId") Long supplierId);
}
