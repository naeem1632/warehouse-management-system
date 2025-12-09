package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Supplier;
import com.warehouse.wms.entity.SupplierLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {

    List<SupplierLedger> findBySupplierOrderByTransactionDateAsc(Supplier supplier);

    Page<SupplierLedger> findBySupplierOrderByTransactionDateDesc(Supplier supplier, Pageable pageable);

    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.supplier = :supplier " +
           "AND sl.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.transactionDate ASC, sl.id ASC")
    List<SupplierLedger> findBySupplierAndDateRange(@Param("supplier") Supplier supplier,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.supplier = :supplier " +
           "ORDER BY sl.transactionDate DESC, sl.id DESC LIMIT 1")
    Optional<SupplierLedger> findLatestBySupplier(@Param("supplier") Supplier supplier);

    Optional<SupplierLedger> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}